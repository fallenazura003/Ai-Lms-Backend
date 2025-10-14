package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.*;
import com.forsakenecho.learning_management_system.entity.*;
import com.forsakenecho.learning_management_system.enums.CourseCategory;
import com.forsakenecho.learning_management_system.enums.NotificationType;
import com.forsakenecho.learning_management_system.service.AiCourseGeneratorService;
import com.forsakenecho.learning_management_system.service.FileStorageService;

import com.forsakenecho.learning_management_system.enums.CourseAccessType;

import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.EventRepository;
import com.forsakenecho.learning_management_system.repository.LearningProgressRepository; // ✅ Thêm LearningProgressRepository
import com.forsakenecho.learning_management_system.repository.LessonRepository; // ✅ Thêm LessonRepository
import com.forsakenecho.learning_management_system.service.CourseService;
import com.forsakenecho.learning_management_system.service.LearningProgressService; // ✅ Thêm LearningProgressService


import com.forsakenecho.learning_management_system.service.NotificationService;
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
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List; // Import List
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors; // Import Collectors


@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseManagementRepository courseManagementRepository;
    private final LearningProgressRepository learningProgressRepository; // ✅ Inject
    private final LessonRepository lessonRepository; // ✅ Inject
    private final LearningProgressService learningProgressService; // ✅ Inject
    private final FileStorageService fileStorageService;
    private final AiCourseGeneratorService  aiCourseGeneratorService;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    // Helper method to get current user
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    // API lấy danh sách khóa học do giáo viên tạo
    @GetMapping("/courses")
    public ResponseEntity<Page<CourseResponse>> getCreatedCourses(
            Authentication authentication,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<CourseResponse> response = courseService.getCreatedCoursesForTeacher(user.getId(), category, pageable);
        return ResponseEntity.ok(response);
    }

    // API tạo khóa học
    @PostMapping(value = "/courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> createCourse(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "category", required = false) CourseCategory category,
            @RequestParam(value = "idea", required = false) String idea,
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
            title = title != null ? title : "Khóa học từ AI: " + idea;
            description = description != null ? description : "Mô tả được sinh từ AI cho ý tưởng: " + idea;
            price = price != null ? price : 0.0;
            category = category != null ? category : CourseCategory.BUSINESS;
        }

        // Kiểm tra các trường bắt buộc sau khi xử lý AI
        if (title == null || title.trim().isEmpty() ||
                description == null || description.trim().isEmpty() ||
                price == null || category == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin bắt buộc để tạo khóa học (title, description, price, category).");
        }

        Course course = Course.builder()
                .title(title)
                .description(description)
                .price(price)
                .category(category)
                .imageUrl(finalImageUrl)
                .creator(teacher)
                .visible(true)
                .build();

        courseRepository.save(course);

        courseManagementRepository.save(CourseManagement.builder()
                .course(course)
                .user(teacher)
                .accessType(CourseAccessType.CREATED)
                .build());

        // Ghi log Event
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
                    // Ghi log Event khi AI tạo nội dung
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

    // ENDPOINT MỚI: Lấy thống kê chi tiết của khóa học (StudentProgress)
    @GetMapping("/courses/{courseId}/stats")
    public ResponseEntity<CourseStatsResponse> getCourseStats(
            @PathVariable UUID courseId,
            Authentication authentication) {

        User teacher = getCurrentUser(authentication);

        // 1. Lấy thông tin cơ bản khóa học và tổng doanh thu
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khóa học không tồn tại"));

        // Giả định logic tính tổng doanh thu từ CourseService (hoặc Repository)
        // Hiện tại không có logic này, nên tôi sẽ dùng giá trị giả định hoặc tính toán đơn giản:
        Long totalRevenue = courseManagementRepository.countByCourseIdAndAccessType(courseId, CourseAccessType.PURCHASED) * course.getPrice().longValue();

        // 2. Lấy tất cả CourseManagement (đã mua) và LearningProgress
        List<CourseManagement> purchasedEnrollments = courseManagementRepository.findByCourseIdAndAccessType(courseId, CourseAccessType.PURCHASED);
        List<UUID> studentIds = purchasedEnrollments.stream()
                .map(cm -> cm.getUser().getId())
                .collect(Collectors.toList());

        List<LearningProgress> progressList = studentIds.isEmpty()
                ? List.of()
                : learningProgressRepository.findByCourseIdAndStudentIdIn(courseId, studentIds);

        // 3. Lấy tổng số bài học
        int totalLessons = lessonRepository.countByCourseId(courseId);

        // Chuyển danh sách tiến độ thành Map để truy cập nhanh
        Map<UUID, LearningProgress> progressMap = progressList.stream()
                .collect(Collectors.toMap(lp -> lp.getStudent().getId(), lp -> lp));

        // 4. Kết hợp dữ liệu (Mapping)
        List<CourseStatsResponse.StudentProgress> studentProgressList = purchasedEnrollments.stream()
                .map(cm -> {
                    LearningProgress lp = progressMap.getOrDefault(cm.getUser().getId(), null);
                    // Nếu không có tiến độ (lp == null), tạo một đối tượng LearningProgress rỗng
                    if (lp == null) {
                        lp = learningProgressService.getOrCreate(cm.getUser().getId(), courseId);
                    }
                    return CourseStatsResponse.StudentProgress.from(cm, lp, totalLessons);
                })
                .collect(Collectors.toList());

        // 5. Xây dựng Response cuối cùng
        CourseStatsResponse response = CourseStatsResponse.from(
                course.getTitle(),
                totalRevenue,
                studentProgressList
        );

        return ResponseEntity.ok(response);
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

        // Ghi log Event
        eventRepository.save(Event.builder()
                .action("Giáo viên " + teacher.getName() + " đã cập nhật khóa học: " + updatedCourse.getTitle())
                .performedBy(teacher.getName())
                .timestamp(LocalDateTime.now())
                .build());

        // Gửi thông báo đến học sinh đã mua khóa học
        notificationService.sendNotificationToStudentsOfCourse(
                updatedCourse.getId(),
                "Khóa học " + updatedCourse.getTitle() + " vừa được cập nhật.",
                NotificationType.COURSE_UPDATED
        );

        return ResponseEntity.ok(CourseResponse.from(updatedCourse));
    }

    // Endpoint xóa khóa học
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable UUID courseId,
            Authentication authentication) {
        User teacher = getCurrentUser(authentication);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khóa học không tồn tại"));

        courseService.deleteCourse(courseId, teacher.getId());

        // Ghi log Event
        eventRepository.save(Event.builder()
                .action("Giáo viên " + teacher.getName() + " đã xóa khóa học với ID: " + courseId)
                .performedBy(teacher.getName())
                .timestamp(LocalDateTime.now())
                .build());

        // Gửi thông báo đến học sinh đã mua khóa học
        notificationService.sendNotificationToStudentsOfCourse(
                courseId,
                "Khóa học " + course.getTitle() + " đã bị xóa bởi giáo viên.",
                NotificationType.COURSE_DELETED
        );

        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // ENDPOINT MỚI: Toggle Course Visibility cho Teacher
    @PatchMapping("/courses/{id}/toggle-visibility")
    public ResponseEntity<String> toggleCourseVisibility(@PathVariable UUID id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        courseService.toggleCourseVisibility(id, currentUser.getId()); // Gọi service

        // Lấy lại course để ghi log chính xác sau khi đã cập nhật
        Course course = courseRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        eventRepository.save(Event.builder()
                .action("Giáo viên " + currentUser.getName() + " đã cập nhật trạng thái hiển thị khóa học: " + course.getTitle() + " thành " + course.isVisible())
                .performedBy(currentUser.getName())
                .timestamp(LocalDateTime.now())
                .build());
        return ResponseEntity.ok("Visibility updated");
    }

    // Lấy chi tiết khóa học
    @GetMapping("/courses/{courseId}/preview")
    public ResponseEntity<CourseResponse> getCoursePreview(
            @PathVariable UUID courseId,
            Authentication authentication) {

        User teacher = getCurrentUser(authentication);

        Course course = courseService.getCoursePreviewByTeacher(courseId, teacher.getId());

        return ResponseEntity.ok(CourseResponse.from(course));
    }
}
