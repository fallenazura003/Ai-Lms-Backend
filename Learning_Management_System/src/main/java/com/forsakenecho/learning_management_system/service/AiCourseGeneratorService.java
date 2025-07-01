package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.GenerateCourseResponse;
import com.forsakenecho.learning_management_system.enums.CourseCategory;
import org.springframework.beans.factory.annotation.Value; // Import này
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class AiCourseGeneratorService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"; // Bỏ ?key= ở đây

    @Value("${gemini.api.key}") // <-- Inject API Key từ application.properties hoặc biến môi trường
    private String geminiApiKey;

    public AiCourseGeneratorService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(GEMINI_BASE_URL).build(); // Base URL không còn chứa key
        this.objectMapper = objectMapper;
    }

    // Records (như đã định nghĩa trước đó)
    private record GeminiContent(String role, List<Map<String, String>> parts) {}
    private record GeminiRequest(List<GeminiContent> contents, Map<String, Object> generationConfig) {}
    private record CourseData(String title, String description, String category, Double price) {}

    public Mono<GenerateCourseResponse> generate(String idea) {
        // ... (phần responseSchema và generationConfig không đổi) ...
        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "title", Map.of("type", "STRING"),
                        "description", Map.of("type", "STRING"),
                        "category", Map.of("type", "STRING", "enum", List.of(
                                CourseCategory.PROGRAMMING.name(),
                                CourseCategory.DESIGN.name(),
                                CourseCategory.MARKETING.name(),
                                CourseCategory.DATA_SCIENCE.name(),
                                CourseCategory.LANGUAGE.name(),
                                CourseCategory.BUSINESS.name()
                        )),
                        "price", Map.of("type", "NUMBER")
                ),
                "required", List.of("title", "description", "category", "price")
        );

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseSchema", responseSchema
        );

        String prompt = "Generate a course title, description, category, and price (in VND, like 200000.0) based on the idea: '" + idea + "'. " +
                "Category must be one of: PROGRAMMING, DESIGN, MARKETING, DATA_SCIENCE, LANGUAGE, BUSINESS. " +
                "Make sure the price is a reasonable number for a course. Provide the response as a JSON object.";

        GeminiRequest requestPayload = new GeminiRequest(
                List.of(new GeminiContent("user", List.of(Map.of("text", prompt)))),
                generationConfig
        );

        return webClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", geminiApiKey).build()) // <-- SỬ DỤNG geminiApiKey ĐÃ INJECT
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    try {
                        String jsonString = jsonNode.at("/candidates/0/content/parts/0/text").asText();

                        CourseData courseData = objectMapper.readValue(jsonString, CourseData.class);

                        CourseCategory predictedCategory;
                        try {
                            predictedCategory = CourseCategory.valueOf(courseData.category());
                        } catch (IllegalArgumentException e) {
                            System.err.println("Gemini returned invalid category: " + courseData.category() + ". Defaulting to BUSINESS.");
                            predictedCategory = CourseCategory.BUSINESS;
                        }

                        return GenerateCourseResponse.builder()
                                .title(courseData.title())
                                .description(courseData.description())
                                .category(predictedCategory)
                                .price(courseData.price())
                                .build();

                    } catch (Exception e) {
                        System.err.println("Error parsing Gemini response or building GenerateCourseResponse: " + e.getMessage());
                        e.printStackTrace();
                        return GenerateCourseResponse.builder()
                                .title("Mặc định: Khóa học về " + idea)
                                .description("Mặc định: Không thể tạo khóa học AI. Vui lòng thử lại.")
                                .category(CourseCategory.BUSINESS)
                                .price(100000.0)
                                .build();
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Error calling Gemini API: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(GenerateCourseResponse.builder()
                            .title("Mặc định: Khóa học về " + idea)
                            .description("Mặc định: Không thể tạo khóa học AI. Vui lòng thử lại.")
                            .category(CourseCategory.BUSINESS)
                            .price(100000.0)
                            .build());
                });
    }
}