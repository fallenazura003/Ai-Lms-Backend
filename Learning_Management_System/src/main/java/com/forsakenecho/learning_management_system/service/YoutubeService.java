package com.forsakenecho.learning_management_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YoutubeService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/search";

    private record VideoCandidate(String videoId, String title, String description, int relevanceScore) {}

    public Mono<String> fetchYoutubeVideoId(String topic) {
        String cleanedTopic = topic.replaceAll("Tuần \\d+:\\s*", "").trim();
        List<String> keywordsToTry = generateSearchKeywords(cleanedTopic);

        // ✅ Ưu tiên các từ khóa có độ phủ rộng hơn trước, sau đó mới đến các từ khóa cụ thể
        // Sắp xếp các từ khóa để các từ khóa chung và phổ biến được thử trước
        keywordsToTry.sort((k1, k2) -> {
            // Ưu tiên từ khóa tiếng Việt ngắn gọn, sau đó đến tiếng Anh ngắn gọn, sau đó là các biến thể dài hơn
            boolean k1IsVietnamese = k1.matches(".*[ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝàáâãèéêìíòóôõùúýĂăĐđĨĩŨũƠơƯưẠạẢảẤấẦầẨầẪẫẬặẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰựỲỳỴỵỶỷỸỹ].*");
            boolean k2IsVietnamese = k2.matches(".*[ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝàáâãèéêìíòóôõùúýĂăĐđĨĩŨũƠơƯưẠạẢảẤấẦầẨầẪẫẬặẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰựỲỳỴỵỶỷỸỹ].*");

            if (k1IsVietnamese && !k2IsVietnamese) return -1; // Ưu tiên tiếng Việt
            if (!k1IsVietnamese && k2IsVietnamese) return 1;

            // Trong cùng một ngôn ngữ, ưu tiên từ khóa ngắn hơn (thường chung hơn)
            return Integer.compare(k1.length(), k2.length());
        });


        return Mono.defer(() -> findVideoWithKeywords(keywordsToTry, 0, cleanedTopic));
    }

    private List<String> generateSearchKeywords(String topic) {
        Set<String> generatedKeywords = new HashSet<>();

        String searchTopic = topic.replaceAll("Bài \\d+:\\s*", "").trim();
        searchTopic = searchTopic.replaceAll("Chương \\d+:\\s*", "").trim();

        // Tier 1: Từ khóa tiếng Việt cốt lõi và phổ biến nhất
        generatedKeywords.add(searchTopic);
        generatedKeywords.add(searchTopic + " hướng dẫn");
        generatedKeywords.add("học " + searchTopic);
        generatedKeywords.add("cách " + searchTopic);
        generatedKeywords.add(searchTopic + " cơ bản");

        // Tier 2: Từ khóa tiếng Anh cốt lõi và phổ biến nhất
        generatedKeywords.add(searchTopic + " tutorial");
        generatedKeywords.add("learn " + searchTopic);
        generatedKeywords.add(searchTopic + " basics");

        // Tier 3: Các biến thể tiếng Việt và tiếng Anh dài hơn/ít phổ biến hơn
        generatedKeywords.add(searchTopic + " cho người mới bắt đầu");
        generatedKeywords.add(searchTopic + " tiếng Việt");
        generatedKeywords.add("tìm hiểu " + searchTopic);
        generatedKeywords.add("khóa học " + searchTopic);
        generatedKeywords.add("bài giảng " + searchTopic);

        generatedKeywords.add(searchTopic + " explained");
        generatedKeywords.add(searchTopic + " course");
        generatedKeywords.add(searchTopic + " lesson");
        generatedKeywords.add("how to " + searchTopic);


        // Tách các từ khóa chính và tạo biến thể linh hoạt hơn (vẫn cần thiết)
        String[] parts = searchTopic.split("[\\s,-]+| và | hoặc ");
        List<String> importantWords = Arrays.stream(parts)
                .map(p -> p.replaceAll("[^a-zA-Z0-9ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝàáâãèéêìíòóôõùúýĂăĐđĨĩŨũƠơƯưẠạẢảẤấẦầẨầẪẫẬặẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰựỲỳỴỵỶỷỸỹ]", "").trim())
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toList());

        if (!importantWords.isEmpty()) {
            String combinedWords = String.join(" ", importantWords);
            generatedKeywords.add(combinedWords);
            generatedKeywords.add(combinedWords + " hướng dẫn");
            generatedKeywords.add(combinedWords + " bài tập"); // Vẫn giữ cho các chủ đề có thể có bài tập
            generatedKeywords.add(combinedWords + " tutorial");
            generatedKeywords.add(combinedWords + " course");

            // Thêm các từ khóa chuyên biệt hơn nếu chủ đề rõ ràng thuộc lĩnh vực đó
            // Phần này vẫn giữ để đảm bảo độ chính xác cho các chủ đề cụ thể
            if (topic.toLowerCase().contains("lập trình") || topic.toLowerCase().contains("code") ||
                    topic.toLowerCase().contains("software") || topic.toLowerCase().contains("phần mềm") ||
                    topic.toLowerCase().contains("java") || topic.toLowerCase().contains("python") ||
                    topic.toLowerCase().contains("javascript") || topic.toLowerCase().contains("c++") ||
                    topic.toLowerCase().contains("web") || topic.toLowerCase().contains("dữ liệu") ||
                    topic.toLowerCase().contains("data")) {
                generatedKeywords.add(searchTopic + " programming");
                generatedKeywords.add(searchTopic + " development");
                generatedKeywords.add(searchTopic + " language");
                if (topic.toLowerCase().contains("java")) generatedKeywords.add("java tutorial");
                if (topic.toLowerCase().contains("python")) generatedKeywords.add("python tutorial");
            }

            if (topic.toLowerCase().contains("tập luyện") || topic.toLowerCase().contains("thể dục") ||
                    topic.toLowerCase().contains("fitness") || topic.toLowerCase().contains("cơ bắp") ||
                    topic.toLowerCase().contains("giảm cân")) {
                generatedKeywords.add(searchTopic + " workout");
                generatedKeywords.add(searchTopic + " exercises");
                generatedKeywords.add(searchTopic + " training");
            }
        }

        return new ArrayList<>(generatedKeywords);
    }

    private Mono<String> findVideoWithKeywords(List<String> keywords, int index, String originalCleanedTopic) {
        // ✅ Thêm giới hạn số lần thử tìm kiếm để tiết kiệm quota
        // Chúng ta sẽ chỉ thử một số lượng từ khóa nhất định (ví dụ: 5-7 từ khóa có tiềm năng nhất)
        // Nếu không tìm thấy, hệ thống sẽ trả về rỗng thay vì thử hết tất cả.
        // Điều này giúp tránh lãng phí quota cho các từ khóa ít khả năng.
        final int MAX_SEARCH_ATTEMPTS = 5; // ✅ Đặt giới hạn số lần thử

        if (index >= keywords.size() || index >= MAX_SEARCH_ATTEMPTS) {
            System.out.println("Không tìm thấy YouTube video ID sau khi thử " + Math.min(keywords.size(), MAX_SEARCH_ATTEMPTS) + " từ khóa.");
            return Mono.just("");
        }

        String currentKeyword = keywords.get(index);
        String encodedQuery = URLEncoder.encode(currentKeyword, StandardCharsets.UTF_8);

        String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL)
                .queryParam("part", "snippet")
                // ✅ Giảm maxResults xuống 5-10 để tiết kiệm băng thông và tài nguyên xử lý (quota không đổi)
                .queryParam("maxResults", 5) // ✅ Giảm maxResults xuống 7
                .queryParam("type", "video")
                .queryParam("videoEmbeddable", "true")
                .queryParam("relevanceLanguage", "vi") // Vẫn ưu tiên tiếng Việt
                .queryParam("regionCode", "VN") // Vẫn ưu tiên khu vực VN
                .queryParam("key", youtubeApiKey)
                .toUriString();

        WebClient client = webClientBuilder.build();

        System.out.println("Đang tìm kiếm YouTube với từ khóa: '" + currentKeyword + "' (Lần thử " + (index + 1) + "/" + MAX_SEARCH_ATTEMPTS + ")");

        return client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    List<VideoCandidate> candidates = new ArrayList<>();
                    try {
                        if (json.has("items") && json.get("items").isArray()) {
                            for (JsonNode item : json.get("items")) {
                                JsonNode videoIdNode = item.at("/id/videoId");
                                JsonNode titleNode = item.at("/snippet/title");
                                JsonNode descriptionNode = item.at("/snippet/description");

                                if (videoIdNode.isTextual() && !videoIdNode.asText().isEmpty() && titleNode.isTextual()) {
                                    String videoId = videoIdNode.asText();
                                    String title = titleNode.asText();
                                    String description = descriptionNode.isTextual() ? descriptionNode.asText() : "";

                                    int score = calculateRelevanceScore(title, description, originalCleanedTopic, currentKeyword);
                                    candidates.add(new VideoCandidate(videoId, title, description, score));
                                }
                            }
                        }

                        if (!candidates.isEmpty()) {
                            candidates.sort(Comparator.comparingInt(VideoCandidate::relevanceScore).reversed());

                            for (VideoCandidate candidate : candidates) {
                                // Ngưỡng điểm để lựa chọn video phù hợp
                                if (candidate.relevanceScore() >= 1) { // Vẫn giữ ngưỡng này để tìm được video dễ hơn
                                    System.out.println("Tìm thấy YouTube video ID phù hợp: " + candidate.videoId() + " (Điểm: " + candidate.relevanceScore() + ", Tiêu đề: " + candidate.title() + ", Từ khóa: '" + currentKeyword + "')");
                                    return candidate.videoId();
                                }
                            }
                        }
                        return "";
                    } catch (Exception e) {
                        System.err.println("Lỗi khi phân tích phản hồi YouTube API hoặc lựa chọn video cho từ khóa '" + currentKeyword + "': " + e.getMessage());
                        return "";
                    }
                })
                .flatMap(videoId -> {
                    if (videoId.isEmpty()) {
                        return findVideoWithKeywords(keywords, index + 1, originalCleanedTopic);
                    }
                    return Mono.just(videoId);
                })
                .onErrorResume(e -> {
                    System.err.println("Lỗi khi gọi YouTube API cho từ khóa '" + currentKeyword + "': " + e.getMessage());
                    return findVideoWithKeywords(keywords, index + 1, originalCleanedTopic);
                });
    }

    private int calculateRelevanceScore(String videoTitle, String videoDescription, String originalTopic, String currentSearchKeyword) {
        int score = 0;
        String lowerCaseVideoTitle = videoTitle.toLowerCase();
        String lowerCaseVideoDescription = videoDescription.toLowerCase();
        String lowerCaseOriginalTopic = originalTopic.toLowerCase();
        String lowerCaseCurrentSearchKeyword = currentSearchKeyword.toLowerCase();

        Set<String> originalTopicWords = Arrays.stream(lowerCaseOriginalTopic.split("[\\s,-]+| và | hoặc "))
                .filter(s -> !s.isEmpty() && s.length() > 2)
                .collect(Collectors.toSet());

        Set<String> currentSearchKeywordWords = Arrays.stream(lowerCaseCurrentSearchKeyword.split("[\\s,-]+| và | hoặc "))
                .filter(s -> !s.isEmpty() && s.length() > 2)
                .collect(Collectors.toSet());

        // 1. Khớp hoàn toàn từ khóa tìm kiếm hiện tại trong tiêu đề (Điểm cao nhất)
        if (lowerCaseVideoTitle.contains(lowerCaseCurrentSearchKeyword)) {
            score += 15;
        }

        // 2. Khớp từ khóa gốc đã làm sạch trong tiêu đề
        if (lowerCaseVideoTitle.contains(lowerCaseOriginalTopic)) {
            score += 10;
        }

        // 3. Khớp các từ riêng lẻ của chủ đề gốc trong tiêu đề
        for (String word : originalTopicWords) {
            if (lowerCaseVideoTitle.contains(word)) {
                score += 3;
            }
        }
        for (String word : currentSearchKeywordWords) {
            if (lowerCaseVideoTitle.contains(word)) {
                score += 2;
            }
        }

        // 4. Khớp các từ khóa tìm kiếm trong mô tả (Điểm thấp hơn một chút)
        if (lowerCaseVideoDescription.contains(lowerCaseCurrentSearchKeyword)) {
            score += 6;
        }
        if (lowerCaseVideoDescription.contains(lowerCaseOriginalTopic)) {
            score += 4;
        }
        for (String word : originalTopicWords) {
            if (lowerCaseVideoDescription.contains(word)) {
                score += 1;
            }
        }
        for (String word : currentSearchKeywordWords) {
            if (lowerCaseVideoDescription.contains(word)) {
                score += 1;
            }
        }

        // 5. Thưởng điểm nếu có từ "tutorial", "hướng dẫn", "course", "bài học", "basics", "explained"
        if (lowerCaseVideoTitle.contains("tutorial") || lowerCaseVideoTitle.contains("hướng dẫn") ||
                lowerCaseVideoTitle.contains("course") || lowerCaseVideoTitle.contains("bài học") ||
                lowerCaseVideoTitle.contains("learn") || lowerCaseVideoTitle.contains("học") ||
                lowerCaseVideoTitle.contains("basics") || lowerCaseVideoTitle.contains("explained")) {
            score += 5;
        }

        // 6. Thưởng điểm nếu video có thể là một bài giảng, giáo trình
        if (lowerCaseVideoTitle.contains("giáo trình") || lowerCaseVideoTitle.contains("bài giảng") ||
                lowerCaseVideoTitle.contains("series")) {
            score += 3;
        }

        return Math.max(0, score);
    }
}