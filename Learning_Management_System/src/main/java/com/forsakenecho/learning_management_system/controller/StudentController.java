package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.*;
import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.entity.TransactionHistory;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import com.forsakenecho.learning_management_system.enums.NotificationType;
import com.forsakenecho.learning_management_system.enums.TransactionType;
import com.forsakenecho.learning_management_system.repository.*;
import com.forsakenecho.learning_management_system.service.CourseService;
import com.forsakenecho.learning_management_system.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    // --- CẤU HÌNH CHIẾT KHẤU & ADMIN ID ---
    // 10% chiết khấu cho Admin (để duy trì hệ thống)
    private static final BigDecimal ADMIN_COMMISSION_RATE = new BigDecimal("0.10");
    private static final int DECIMAL_SCALE = 2; // Làm tròn đến 2 chữ số thập phân

    // ID Cố định của Super Admin theo thông tin bạn cung cấp
    private static final UUID SYSTEM_ADMIN_ID = UUID.fromString("08eebccd-8460-460a-b23d-3b731846feaf");

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseManagementRepository courseManagementRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final UserRepository userRepository;
    private final NotificationService  notificationService;


    // Helper method to get current user
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    // Helper method để tìm kiếm Admin BẰNG ID cố định
    private User getSystemAdmin() {
        return userRepository.findById(SYSTEM_ADMIN_ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy tài khoản Admin hệ thống với ID cố định."));
    }

    // Lấy danh sách khóa học đã mua (KHÔNG CẦN LỌC THEO VISIBLE Ở ĐÂY, vì đã mua thì luôn thấy)
    @GetMapping("/courses")
    public ResponseEntity<Page<CourseResponse>> getPurchasedCourses(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<Course> courses = courseService.getCoursesByUserAndAccessType(user.getId(), CourseAccessType.PURCHASED, pageable);
        Page<CourseResponse> response = courses.map(CourseResponse::from);
        return ResponseEntity.ok(response);
    }

    // Mua khóa học
    @Transactional
    @PostMapping("/purchase")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> purchaseCourse(
            @RequestBody PurchaseCourseRequest request,
            Authentication authentication) {

        User student = getCurrentUser(authentication);

        // Fetch course and its creator (teacher) within the transaction
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khóa học không tồn tại"));

        User teacher = course.getCreator(); // Get the teacher (creator of the course)
        if (teacher == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy thông tin giáo viên cho khóa học này.");
        }

        // Lấy tài khoản Admin hệ thống
        User admin = getSystemAdmin();


        // ✅ Không cho mua khóa học bị ẩn
        if (!course.isVisible()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<PurchaseResponse>builder()
                    .message("Khóa học này hiện không khả dụng để mua.")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        // ✅ Kiểm tra đã mua hay chưa
        boolean alreadyPurchased = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), course.getId(), CourseAccessType.PURCHASED)
                .isPresent();

        if (alreadyPurchased) {
            return ResponseEntity.badRequest().body(ApiResponse.<PurchaseResponse>builder()
                    .message("Bạn đã mua khóa học này rồi.")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        // ✅ Course price is a double, convert it to BigDecimal for calculations
        BigDecimal coursePriceBd = BigDecimal.valueOf(course.getPrice());

        // Check if it's a paid course
        if (coursePriceBd.compareTo(BigDecimal.ZERO) > 0) { // Check if price is greater than 0
            // ✅ studentBalanceBd is already BigDecimal from student.getBalance()
            BigDecimal studentBalanceBd = student.getBalance(); // student.getBalance() now returns BigDecimal

            // ✅ Kiểm tra số dư có đủ không
            if (studentBalanceBd.compareTo(coursePriceBd) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<PurchaseResponse>builder()
                        .message("Số dư không đủ để mua khóa học này.")
                        .timestamp(LocalDateTime.now())
                        .build());
            }

            // --- LOGIC CHIẾT KHẤU ---
            // 1. Tính toán phần trăm chiết khấu cho Admin
            BigDecimal adminShare = coursePriceBd.multiply(ADMIN_COMMISSION_RATE)
                    .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);

            // 2. Tính toán phần tiền Giáo viên nhận được
            BigDecimal teacherShare = coursePriceBd.subtract(adminShare);

            // 3. Xử lý trừ tiền sinh viên (Trừ toàn bộ giá khóa học)
            student.setBalance(studentBalanceBd.subtract(coursePriceBd));
            userRepository.save(student);

            // 4. Cộng tiền cho giáo viên (balance là BigDecimal)
            BigDecimal teacherBalanceBd = teacher.getBalance() != null ? teacher.getBalance() : BigDecimal.ZERO;
            teacher.setBalance(teacherBalanceBd.add(teacherShare)); // Cộng phần tiền đã trừ chiết khấu
            userRepository.save(teacher);

            // 5. Cộng tiền cho Admin (balance là BigDecimal)
            BigDecimal adminBalanceBd = admin.getBalance() != null ? admin.getBalance() : BigDecimal.ZERO;
            admin.setBalance(adminBalanceBd.add(adminShare)); // Cộng phần chiết khấu
            userRepository.save(admin);

            // --- Ghi nhận giao dịch ---

            // ✅ Ghi nhận giao dịch của sinh viên (người trả tiền)
            transactionHistoryRepository.save(TransactionHistory.builder()
                    .user(student)
                    .type(TransactionType.PURCHASE)
                    .amount(coursePriceBd.negate()) // Ghi nhận là chi tiêu (âm)
                    .description("Mua khóa học: " + course.getTitle())
                    .createdAt(LocalDateTime.now())
                    .build());

            // ✅ Ghi nhận giao dịch cho giáo viên (người nhận tiền)
            transactionHistoryRepository.save(TransactionHistory.builder()
                    .user(teacher)
                    .type(TransactionType.EARNINGS)
                    .amount(teacherShare) // Chỉ ghi nhận phần tiền đã nhận
                    .description("Thu nhập từ khóa học: " + course.getTitle() + " (sau chiết khấu)")
                    .createdAt(LocalDateTime.now())
                    .build());

            // ✅ Ghi nhận giao dịch cho Admin (người nhận chiết khấu)
            transactionHistoryRepository.save(TransactionHistory.builder()
                    .user(admin)
                    .type(TransactionType.COMMISSION)
                    .amount(adminShare) // Chỉ ghi nhận phần chiết khấu
                    .description("Chiết khấu hệ thống từ khóa học: " + course.getTitle() + " (" + ADMIN_COMMISSION_RATE.multiply(new BigDecimal("100")) + "%)")
                    .createdAt(LocalDateTime.now())
                    .build());


        } else {
            // Khóa học miễn phí, không cần trừ tiền hay tạo transaction cho việc trao đổi tiền
            PurchaseResponse purchaseResponse = PurchaseResponse.builder()
                    .studentId(student.getId())
                    .courseId(course.getId())
                    .purchasedAt(LocalDateTime.now()) // Set current time for free course enrollment
                    .balance(student.getBalance()) // Still return as double for frontend
                    .build();

            ApiResponse<PurchaseResponse> response = ApiResponse.<PurchaseResponse>builder()
                    .message("Đăng ký khóa học miễn phí thành công!")
                    .data(purchaseResponse)
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }

        // ✅ Tạo access cho sinh viên vào khóa học
        CourseManagement courseManagement = CourseManagement.builder()
                .user(student)
                .course(course)
                .accessType(CourseAccessType.PURCHASED)
                .build();
        courseManagementRepository.save(courseManagement);



        // ✅ Cập nhật `PurchaseResponse` để trả về balance mới của student
        PurchaseResponse purchaseResponse = PurchaseResponse.builder()
                .studentId(student.getId())
                .courseId(course.getId())
                .purchasedAt(courseManagement.getPurchasedAt())
                .balance(student.getBalance()) // ✅ TRẢ VỀ SỐ DƯ MỚI CỦA SINH VIÊN (chuyển BigDecimal về Double)
                .build();

        ApiResponse<PurchaseResponse> response = ApiResponse.<ApiResponse<PurchaseResponse>>builder()
                .message("Mua khóa học thành công!") // Message cho toast
                .timestamp(LocalDateTime.now())
                .build().getData();

        // --- CẬP NHẬT LẠI THÔNG BÁO (NOTIFICATION) ---
        // Thông báo đơn giản, không bao gồm số tiền chiết khấu
        notificationService.sendNotification(
                teacher.getEmail(),
                student.getName() + " đã mua khóa học: " + course.getTitle(),
                NotificationType.COURSE_PURCHASED
        );


        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // Lấy chi tiết khóa học (kèm kiểm tra khóa học có bị ẩn không)
    @GetMapping("/courses/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CourseResponse> getCourseDetail(@PathVariable UUID id, Authentication authentication) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User student = getCurrentUser(authentication);

        // Kiểm tra quyền truy cập:
        // 1. Nếu khóa học bị ẩn
        // 2. VÀ học sinh CHƯA mua khóa học đó
        boolean isPurchased = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), course.getId(), CourseAccessType.PURCHASED)
                .isPresent();

        if (!course.isVisible() && !isPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khóa học này hiện không khả dụng.");
        }
        // Nếu khóa học bị ẩn nhưng đã mua, vẫn cho phép truy cập.
        // Nếu khóa học hiển thị, cho phép truy cập.

        return ResponseEntity.ok(CourseResponse.from(course));
    }


    // Lấy danh sách khóa học đang visible nhưng học sinh CHƯA mua (explore)
    @GetMapping("/explore")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<CourseResponse>> getExploreCourses(
            Authentication authentication,
            @RequestParam(value = "category", required = false) String category, // ✅ Thêm category
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        User student = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);

        // ✅ Gọi phương thức mới trong CourseService để lọc explore courses
        Page<CourseResponse> exploreCourses = courseService.getExploreCoursesForStudent(student.getId(), category, pageable);

        return ResponseEntity.ok(exploreCourses);
    }

    // Kiểm tra học sinh đã mua một khóa học cụ thể chưa
    @GetMapping("/enrolled/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Boolean> isStudentEnrolled(@PathVariable UUID courseId, Authentication authentication) {
        User student = getCurrentUser(authentication);

        boolean isEnrolled = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), courseId, CourseAccessType.PURCHASED)
                .isPresent();

        return ResponseEntity.ok(isEnrolled); // CHỈ trả true nếu đã mua
    }
}
