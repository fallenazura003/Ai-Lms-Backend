package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.*;
import com.forsakenecho.learning_management_system.enums.CourseCategory;
import com.forsakenecho.learning_management_system.service.AiCourseGeneratorService;
import com.forsakenecho.learning_management_system.service.FileStorageService;

import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.entity.Event; // ✅ Import Event
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;

import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.EventRepository; // ✅ Import EventRepository
import com.forsakenecho.learning_management_system.service.CourseService;


import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')") // ✅ Đặt PreAuthorize ở cấp class để áp dụng cho tất cả methods
public class TeacherController {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseManagementRepository courseManagementRepository;
    private final FileStorageService fileStorageService;
    private final AiCourseGeneratorService  aiCourseGeneratorService;
    private final EventRepository eventRepository; // ✅ Inject EventRepository

    // Helper method to get current user
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    // API lấy danh sách khóa học do giáo viên tạo
    @GetMapping("/courses")
    public ResponseEntity<?> getCreatedCourses(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<Course> courses = courseService.getCoursesByUserAndAccessType(user.getId(), CourseAccessType.CREATED, pageable);
        Page<CourseResponse> response = courses.map(CourseResponse::from);
        return ResponseEntity.ok(response);
    }

    // API tạo khóa học
    @PostMapping(value = "/courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCourse(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "category", required = false) CourseCategory category,
            @RequestParam(value = "idea", required = false) String idea, // Được dùng khi sinh từ AI
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "imageUrl", required = false) String externalImageUrl,
            Authentication authentication
    ) throws IOException {

        User teacher = getCurrentUser(authentication);
        String finalImageUrl = null;

        if (imageFile != null && !imageFile.isEmpty()) {
            finalImageUrl = fileStorageService.save(imageFile);
        } else if (externalImageUrl != null && !externalImageUrl.trim().isEmpty()) {
            finalImageUrl = externalImageUrl;
        }

        // Logic xử lý khi có idea (sinh từ AI)
        if (idea != null && !idea.trim().isEmpty()) {
            // Đây là một ví dụ đơn giản. Trong thực tế, bạn có thể gọi
            // aiCourseGeneratorService.generate(idea) để nhận đầy đủ thông tin
            // và sử dụng thông tin đó để tạo Course.
            // Nếu bạn muốn AI tự động tạo Course hoàn toàn, cân nhắc tách thành một endpoint riêng.
            title = title != null ? title : "Khóa học từ AI: " + idea;
            description = description != null ? description : "Mô tả được sinh từ AI cho ý tưởng: " + idea;
            price = price != null ? price : 0.0;
            category = category != null ? category : CourseCategory.BUSINESS; // Cần một category mặc định hoặc từ AI
        }

        // Kiểm tra các trường bắt buộc sau khi xử lý AI
        if (title == null || title.trim().isEmpty() ||
                description == null || description.trim().isEmpty() ||
                price == null || category == null) {
            return ResponseEntity.badRequest().body("Thiếu thông tin bắt buộc để tạo khóa học (title, description, price, category).");
        }

        Course course = Course.builder()
                .title(title)
                .description(description)
                .price(price)
                .category(category)
                .imageUrl(finalImageUrl)
                .creator(teacher)
                .visible(true) // Mặc định hiển thị khi tạo
                .build();

        courseRepository.save(course);

        courseManagementRepository.save(CourseManagement.builder()
                .course(course)
                .user(teacher)
                .accessType(CourseAccessType.CREATED)
                .build());

        // ✅ Ghi log Event
        eventRepository.save(Event.builder()
                .action("Giáo viên " + teacher.getName() + " đã tạo khóa học mới: " + course.getTitle())
                .performedBy(teacher.getName())
                .timestamp(LocalDateTime.now())
                .build());

        CourseResponse courseResponse = CourseResponse.from(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(courseResponse);
    }

    @PostMapping("/courses/generate")
    public Mono<ResponseEntity<GenerateCourseResponse>> generateCourseFromIdea(@RequestBody Map<String, String> request, Authentication authentication) {
        String idea = request.get("idea");
        User teacher = getCurrentUser(authentication);

        if (idea == null || idea.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(null));
        }
        return aiCourseGeneratorService.generate(idea)
                .doOnSuccess(response -> {
                    // ✅ Ghi log Event khi AI tạo nội dung
                    eventRepository.save(Event.builder()
                            .action("Giáo viên " + teacher.getName() + " đã yêu cầu AI sinh nội dung khóa học cho ý tưởng: " + idea)
                            .performedBy(teacher.getName())
                            .timestamp(LocalDateTime.now())
                            .build());
                })
                .map(response -> ResponseEntity.ok(response))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Endpoint lấy chi tiết một khóa học
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<CourseResponse> getCourseById(
            @PathVariable UUID courseId,
            Authentication authentication) {
        User teacher = getCurrentUser(authentication);
        Course course = courseService.getCourseByIdForTeacher(courseId, teacher.getId());
        return ResponseEntity.ok(CourseResponse.from(course));
    }

    // Endpoint cập nhật khóa học
    @PutMapping(value = "/courses/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable UUID courseId,
            @RequestParam(value = "title") String title,
            @RequestParam(value = "description") String description,
            @RequestParam(value = "price") Double price,
            @RequestParam(value = "category") CourseCategory category,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String externalImageUrl,
            Authentication authentication
    ) throws IOException {
        User teacher = getCurrentUser(authentication);

        CreateCourseRequest updateRequest = new CreateCourseRequest(title, description, category, price, null);

        Course updatedCourse = courseService.updateCourse(courseId, updateRequest, teacher, imageFile, externalImageUrl);

        // ✅ Ghi log Event
        eventRepository.save(Event.builder()
                .action("Giáo viên " + teacher.getName() + " đã cập nhật khóa học: " + updatedCourse.getTitle())
                .performedBy(teacher.getName())
                .timestamp(LocalDateTime.now())
                .build());

        return ResponseEntity.ok(CourseResponse.from(updatedCourse));
    }

    // Endpoint xóa khóa học
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable UUID courseId,
            Authentication authentication) {
        User teacher = getCurrentUser(authentication);
        courseService.deleteCourse(courseId, teacher.getId());

        // ✅ Ghi log Event
        eventRepository.save(Event.builder()
                .action("Giáo viên " + teacher.getName() + " đã xóa khóa học với ID: " + courseId)
                .performedBy(teacher.getName())
                .timestamp(LocalDateTime.now())
                .build());

        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // ✅ ENDPOINT MỚI: Toggle Course Visibility cho Teacher
    @PatchMapping("/courses/{id}/toggle-visibility")
    public ResponseEntity<?> toggleCourseVisibility(@PathVariable UUID id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        courseService.toggleCourseVisibility(id, currentUser.getId()); // Gọi service

        // Lấy lại course để ghi log chính xác sau khi đã cập nhật
        Course course = courseRepository.findById(id).orElseThrow();

        eventRepository.save(Event.builder()
                .action("Giáo viên " + currentUser.getName() + " đã cập nhật trạng thái hiển thị khóa học: " + course.getTitle() + " thành " + course.isVisible())
                .performedBy(currentUser.getName())
                .timestamp(LocalDateTime.now())
                .build());
        return ResponseEntity.ok("Visibility updated");
    }
}