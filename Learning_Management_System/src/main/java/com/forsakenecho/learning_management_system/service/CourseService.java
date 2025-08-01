// CourseService.java
package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.CourseResponse;
import com.forsakenecho.learning_management_system.dto.CreateCourseRequest;
import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import com.forsakenecho.learning_management_system.enums.Role;
import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseManagementRepository courseManagementRepository;
    private final CourseRepository courseRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public Course createCourse(CreateCourseRequest request, User creator, MultipartFile imageFile, String externalImageUrl) throws IOException {
        Course newCourse = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .creator(creator)
                .visible(false) // Mặc định là ẩn khi mới tạo
                .build();

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = fileStorageService.save(imageFile);
        } else if (externalImageUrl != null && !externalImageUrl.trim().isEmpty()) {
            imageUrl = externalImageUrl;
        }
        newCourse.setImageUrl(imageUrl);

        Course savedCourse = courseRepository.save(newCourse);

        // Tự động gán quyền CREATED cho người tạo khóa học
        CourseManagement courseManagement = CourseManagement.builder()
                .user(creator)
                .course(savedCourse)
                .accessType(CourseAccessType.CREATED)
                .build();
        courseManagementRepository.save(courseManagement);

        return savedCourse;
    }



    // PHƯƠNG THỨC NÀY ĐƯỢC SỬ DỤNG CHO DANH SÁCH KHÓA HỌC ĐÃ MUA CÓ PHÂN TRANG
    public Page<Course> getCoursesByUserAndAccessType(UUID userId, CourseAccessType type, Pageable pageable) {
        if (type == CourseAccessType.PURCHASED) {
            // Sử dụng phương thức mới chỉ lấy các khóa học đã mua VÀ visible
            // Lưu ý: findByUserIdAndAccessTypeAndCourseVisibleTrue cần được định nghĩa trong CourseManagementRepository
            // Hoặc bạn có thể dùng Specification ở đây nếu muốn linh hoạt hơn
            return courseManagementRepository.findByUserIdAndAccessTypeAndCourseVisibleTrue(userId, type, pageable)
                    .map(CourseManagement::getCourse);
        } else if (type == CourseAccessType.CREATED) {
            // Đối với khóa học do giáo viên tạo, họ phải luôn thấy tất cả khóa học của mình (visible hoặc ẩn)
            // Phương thức này sẽ được thay thế bằng getCreatedCoursesForTeacher nếu có category filter
            return courseRepository.findByCreatorId(userId, pageable);
        }
        // Đối với các loại truy cập khác (nếu có), bạn có thể thêm logic tương ứng
        // Mặc định, trả về rỗng hoặc ném ngoại lệ nếu loại không được hỗ trợ
        return Page.empty(pageable);
    }

    // Phương thức này có thể không cần thiết nữa nếu dùng getExploreCoursesForStudent
    public Page<Course> getVisibleCoursesNotPurchased(UUID studentId, Pageable pageable) {
        List<UUID> purchasedIds = courseManagementRepository
                .findByUserIdAndAccessType(studentId, CourseAccessType.PURCHASED)
                .stream()
                .map(cm -> cm.getCourse().getId())
                .toList();

        if (purchasedIds.isEmpty()) {
            return courseRepository.findByVisibleTrue(pageable);
        } else {
            return courseRepository.findByVisibleTrueAndIdNotIn(purchasedIds, pageable);
        }
    }

    public Course getCourseByIdForTeacher(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với ID: " + courseId));

        if (!course.getCreator().getId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập khóa học này.");
        }
        return course;
    }

    public Course updateCourse(UUID courseId, CreateCourseRequest request, User teacher, MultipartFile imageFile, String externalImageUrl) throws IOException {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với ID: " + courseId));

        if (!existingCourse.getCreator().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật khóa học này.");
        }

        existingCourse.setTitle(request.getTitle());
        existingCourse.setDescription(request.getDescription());
        existingCourse.setPrice(request.getPrice());
        existingCourse.setCategory(request.getCategory());

        String currentImageUrl = existingCourse.getImageUrl();
        String finalImageUrl = currentImageUrl;

        if (imageFile != null && !imageFile.isEmpty()) {
            if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                fileStorageService.delete(currentImageUrl);
            }
            finalImageUrl = fileStorageService.save(imageFile);
        } else if (externalImageUrl != null && !externalImageUrl.trim().isEmpty()) {
            if (!externalImageUrl.equals(currentImageUrl)) {
                if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                    fileStorageService.delete(currentImageUrl);
                }
                finalImageUrl = externalImageUrl;
            }
        }
        existingCourse.setImageUrl(finalImageUrl);

        return courseRepository.save(existingCourse);
    }

    public void deleteCourse(UUID courseId, UUID teacherId) {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với ID: " + courseId));

        if (!existingCourse.getCreator().getId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa khóa học này.");
        }

        if (existingCourse.getImageUrl() != null && !existingCourse.getImageUrl().isEmpty()) {
            fileStorageService.delete(existingCourse.getImageUrl());
        }

        courseRepository.delete(existingCourse);
    }

    @Transactional
    public void toggleCourseVisibility(UUID courseId, UUID userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học."));

        // Chỉ cho phép người tạo khóa học (teacher) hoặc Admin thay đổi trạng thái hiển thị
        User performingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại."));

        if (!course.getCreator().getId().equals(userId)
                && performingUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền.");
        }

        course.setVisible(!course.isVisible());
        courseRepository.save(course);
    }

    public Course getCoursePreviewByTeacher(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học."));

        // Chỉ cho phép xem nếu người gọi là giáo viên tạo khóa học
        if (!course.getCreator().getId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem trước khóa học này.");
        }

        return course;
    }

    // Cập nhật phương thức searchCourses (vẫn dùng cho Public search)
    public Page<CourseResponse> searchCourses(String keyword, String category, Pageable pageable) {
        Specification<Course> spec = Specification.where(null); // Bắt đầu với một Specification rỗng

        // 1. Luôn lọc theo visible = true
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("visible")));

        // 2. Lọc theo keyword (tìm kiếm trong title hoặc description)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerCaseKeyword = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("title")), lowerCaseKeyword),
                            cb.like(cb.lower(root.get("description")), lowerCaseKeyword)
                    )
            );
        }

        // 3. Lọc theo category
        if (category != null && !category.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }

        Page<Course> coursesPage = courseRepository.findAll(spec, pageable); // Sử dụng findAll với Specification
        return coursesPage.map(CourseResponse::from);
    }

    // PHƯƠNG THỨC MỚI: Lấy các khóa học để sinh viên "khám phá" (chưa mua, visible, có thể lọc theo category)
    public Page<CourseResponse> getExploreCoursesForStudent(UUID studentId, String category, Pageable pageable) {
        // Lấy danh sách ID các khóa học đã mua bởi sinh viên
        List<UUID> purchasedCourseIds = courseManagementRepository
                .findByUserIdAndAccessType(studentId, CourseAccessType.PURCHASED)
                .stream()
                .map(cm -> cm.getCourse().getId())
                .collect(Collectors.toList());

        Specification<Course> spec = Specification.where(null);

        // 1. Luôn lọc các khóa học visible
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("visible")));

        // 2. Loại trừ các khóa học đã mua
        if (!purchasedCourseIds.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("id").in(purchasedCourseIds).not());
        }

        // 3. Lọc theo category nếu có
        if (category != null && !category.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }

        Page<Course> coursesPage = courseRepository.findAll(spec, pageable);
        return coursesPage.map(CourseResponse::from);
    }


    // PHƯƠNG THỨC MỚI: Lấy các khóa học đã tạo bởi giáo viên (có thể lọc theo category)
    public Page<CourseResponse> getCreatedCoursesForTeacher(UUID teacherId, String category, Pageable pageable) {
        Specification<Course> spec = Specification.where(null);

        // 1. Lọc theo người tạo (giáo viên)
        spec = spec.and((root, query, cb) -> cb.equal(root.get("creator").get("id"), teacherId));

        // 2. Lọc theo category nếu có (Giáo viên muốn thấy tất cả khóa học của họ, ẩn hay không ẩn, chỉ lọc theo category)
        if (category != null && !category.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }

        Page<Course> coursesPage = courseRepository.findAll(spec, pageable);
        return coursesPage.map(CourseResponse::from);
    }

    // Phương thức để lấy tất cả các danh mục duy nhất
    public List<String> getAllDistinctCategories() {
        return courseRepository.findDistinctCategories();
    }

    // PHƯƠNG THỨC MỚI: Lấy ID của các khóa học đã mua bởi một sinh viên
    public List<UUID> getPurchasedCourseIds(UUID studentId) {
        return courseManagementRepository.findByUserIdAndAccessType(studentId, CourseAccessType.PURCHASED)
                .stream()
                .map(cm -> cm.getCourse().getId())
                .collect(Collectors.toList());
    }

    // Ví dụ về phương thức tính rating (nếu bạn có)
    public double getAverageRatingForCourse(UUID courseId) {
        // Giả định bạn có ReviewRepository và một cách để lấy đánh giá
        // Ví dụ đơn giản:
        // List<Review> reviews = reviewRepository.findByCourseId(courseId);
        // return reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);
        return 4.5; // Thay thế bằng logic thực tế của bạn
    }
}