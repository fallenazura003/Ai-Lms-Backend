package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.GenerateLessonResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class AiLessonGeneratorService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final YoutubeService youtubeService; // Đã có

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public AiLessonGeneratorService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, YoutubeService youtubeService) {
        this.webClient = webClientBuilder.baseUrl(GEMINI_BASE_URL).build();
        this.objectMapper = objectMapper;
        this.youtubeService = youtubeService;
    }

    private record GeminiContent(String role, List<Map<String, String>> parts) {}
    private record GeminiRequest(List<GeminiContent> contents, Map<String, Object> generationConfig) {}
    // ✅ Loại bỏ youtubeVideoId khỏi LessonData vì nó sẽ được YoutubeService cung cấp
    private record LessonData(String title, String recallQuestion, String material, String shortAnswer, String multipleChoice, String summaryTask) {}


    public Mono<GenerateLessonResponse> generateLesson(String courseTitle, String lessonIdea) {
        // ✅ Cập nhật responseSchema:
        // - Loại bỏ youtubeVideoId khỏi properties và required
        // - Cập nhật description cho recallQuestion, shortAnswer, multipleChoice để rõ ràng hơn về số lượng
        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "title", Map.of("type", "STRING", "description", "Tiêu đề ngắn gọn cho bài học."),
                        "recallQuestion", Map.of("type", "STRING", "description", "Hai câu hỏi ngắn để kiểm tra khả năng ghi nhớ, mỗi câu trên một dòng."), // ✅ Rõ ràng 2 câu
                        "material", Map.of("type", "STRING", "description", "Tài liệu bài học toàn diện ở định dạng markdown. Sử dụng tiêu đề, dấu đầu dòng và khối code khi thích hợp. Bao gồm ít nhất 300 từ."),
                        "shortAnswer", Map.of("type", "STRING", "description", "Hai câu hỏi trả lời ngắn riêng biệt liên quan đến nội dung bài học, mỗi câu trên một dòng."), // ✅ Rõ ràng 2 câu
                        "multipleChoice", Map.of("type", "STRING", "description",
                                "Mảng JSON gồm 3 câu hỏi trắc nghiệm, mỗi câu có 4 lựa chọn và đáp án đúng. Định dạng: [{'question': '...', 'options': ['A','B','C','D'], 'correctAnswer': 'A'}]."), // ✅ Rõ ràng 3 câu
                        "summaryTask", Map.of("type", "STRING", "description", "Một nhiệm vụ để học sinh tóm tắt các điểm chính của bài học.")
                ),
                "required", List.of("title", "material", "recallQuestion", "shortAnswer", "multipleChoice", "summaryTask") // ✅ youtubeVideoId không còn required ở đây
        );

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseSchema", responseSchema
        );

        // ✅ Tối ưu prompt:
        // - Nhấn mạnh tiếng Việt.
        // - Bỏ yêu cầu youtubeVideoId khỏi prompt Gemini.
        // - Rõ ràng về số lượng câu hỏi ngắn và trắc nghiệm.
        String prompt = String.format(
                "Tạo một kế hoạch bài học và nội dung chi tiết cho khóa học có tiêu đề '%s' dựa trên ý tưởng: '%s'. " +
                        "Phản hồi phải là một đối tượng JSON chứa các trường sau: 'title', 'recallQuestion', 'material', 'shortAnswer', 'multipleChoice', và 'summaryTask'. " +
                        "Đảm bảo toàn bộ nội dung được tạo bằng tiếng Việt.",
                courseTitle, lessonIdea
        ) + String.format(
                "\n\nCác yêu cầu cụ thể cho mỗi trường (Tất cả nội dung bằng tiếng Việt):" +
                        "\n- 'title': Tiêu đề ngắn gọn và rõ ràng cho bài học." +
                        "\n- 'recallQuestion': Tạo HAI câu hỏi gợi nhớ ngắn gọn, mỗi câu hỏi trên một dòng riêng biệt." +
                        "\n- 'material': Nội dung bài học chi tiết, sử dụng định dạng markdown (tiêu đề, dấu đầu dòng, khối code). Độ dài ít nhất 300 từ. Trình bày rõ ràng, dễ đọc." +
                        "\n- 'shortAnswer': Tạo HAI câu hỏi trả lời ngắn riêng biệt. Mỗi câu hỏi trên một dòng riêng biệt." +
                        "\n- 'multipleChoice': Tạo BA câu hỏi trắc nghiệm. Mỗi câu hỏi phải có 4 lựa chọn (A, B, C, D) và một 'correctAnswer'. Định dạng là một mảng JSON của các đối tượng, ví dụ: " +
                        "[{'question': 'Câu hỏi 1?', 'options': ['A','B','C','D'], 'correctAnswer': 'A'}, {'question': 'Câu hỏi 2?', 'options': ['X','Y','Z','W'], 'correctAnswer': 'X'}, {'question': 'Câu hỏi 3?', 'options': ['1','2','3','4'], 'correctAnswer': '1'}]." +
                        "\n- 'summaryTask': Một nhiệm vụ rõ ràng để học sinh tóm tắt các điểm chính của bài học." +
                        "\n\nĐảm bảo tất cả các trường chuỗi được thoát đúng cách cho JSON."
        );

        GeminiRequest requestPayload = new GeminiRequest(
                List.of(new GeminiContent("user", List.of(Map.of("text", prompt)))),
                generationConfig
        );

        return webClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", geminiApiKey).build())
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    try {
                        String jsonString = jsonNode.at("/candidates/0/content/parts/0/text").asText();
                        return objectMapper.readValue(jsonString, LessonData.class);
                    } catch (Exception e) {
                        System.err.println("Lỗi khi phân tích phản hồi Gemini: " + e.getMessage());
                        e.printStackTrace();
                        return null; // Trả về null để flatMap xử lý fallback
                    }
                })
                .flatMap(lessonData -> {
                    if (lessonData == null) {
                        return Mono.just(generateFallback(lessonIdea, "Lỗi phân tích phản hồi từ AI."));
                    }
                    // ✅ Gọi YoutubeService để tìm video dựa trên title của bài học
                    return youtubeService.fetchYoutubeVideoId(lessonData.title())
                            .map(videoId -> GenerateLessonResponse.builder()
                                    .title(lessonData.title())
                                    .youtubeVideoId(videoId) // ✅ Gán videoId từ YoutubeService
                                    .recallQuestion(lessonData.recallQuestion())
                                    .material(lessonData.material())
                                    .shortAnswer(lessonData.shortAnswer())
                                    .multipleChoice(lessonData.multipleChoice())
                                    .summaryTask(lessonData.summaryTask())
                                    .build()
                            );
                })
                .onErrorResume(e -> {
                    System.err.println("Lỗi khi gọi API Gemini hoặc YoutubeService: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(generateFallback(lessonIdea, e.getMessage()));
                });
    }

    // ✅ Cập nhật generateFallback để phù hợp với yêu cầu 2 câu hỏi ngắn, 3 trắc nghiệm và tiếng Việt
    private GenerateLessonResponse generateFallback(String idea, String reason) {
        return GenerateLessonResponse.builder()
                .title("Bài học mặc định về " + idea + " (Lỗi sinh nội dung)")
                .youtubeVideoId("") // Không có video ID hợp lệ nếu có lỗi
                .recallQuestion("Câu hỏi gợi nhớ 1: Có lỗi xảy ra khi tạo nội dung AI.\nCâu hỏi gợi nhớ 2: Vui lòng thử lại hoặc tạo thủ công.")
                .material("## Lỗi tạo nội dung AI.\n\nKhông thể sinh nội dung bài học tự động. Lý do: " + reason + "\n\nVui lòng thử lại hoặc tạo bài học thủ công.")
                .shortAnswer("Câu hỏi ngắn 1: Giải thích nguyên nhân lỗi.\nCâu hỏi ngắn 2: Đề xuất giải pháp khắc phục.")
                .multipleChoice(
                        "[{\"question\":\"Điều gì xảy ra khi AI gặp lỗi?\",\"options\":[\"A. Tạo nội dung sai\",\"B. Không tạo được nội dung\",\"C. Cả A và B\",\"D. Không có gì\"],\"correctAnswer\":\"B\"}," +
                                "{\"question\":\"Bạn nên làm gì khi gặp lỗi?\",\"options\":[\"A. Thử lại\",\"B. Báo cáo lỗi\",\"C. Chuyển sang tạo thủ công\",\"D. Tất cả các phương án trên\"],\"correctAnswer\":\"D\"}," +
                                "{\"question\":\"Mục đích của việc tạo bài học bằng AI là gì?\",\"options\":[\"A. Tiết kiệm thời gian\",\"B. Tự động hóa nội dung\",\"C. Nâng cao chất lượng\",\"D. Tất cả các phương án trên\"],\"correctAnswer\":\"D\"}]"
                )
                .summaryTask("Tóm tắt các bước để xử lý lỗi khi tạo bài học bằng AI.")
                .build();
    }
}