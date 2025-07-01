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
import com.forsakenecho.learning_management_system.repository.UserRepository; // ✅ Thêm import này
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UserRepository userRepository; // ✅ Inject UserRepository để kiểm tra vai trò

    public List<CourseResponse> getCoursesByUserAndAccessType(UUID userId, CourseAccessType accessType){
        // Logic này có vẻ không được dùng với phân trang, cần xem xét lại mục đích
        // Tuy nhiên, nếu nó được dùng, cũng nên xem xét việc lọc visible
        // Hiện tại, tôi sẽ không sửa đổi nó để tránh phá vỡ các phần khác nếu nó có logic riêng
        return courseManagementRepository.findByUserIdAndAccessType(userId, accessType)
                .stream()
                .map(CourseManagement::getCourse)
                .filter(Course::isVisible) // Giữ lại bộ lọc này nếu muốn visible
                .map(c -> CourseResponse.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .description(c.getDescription())
                        .price(c.getPrice())
                        .creatorName(c.getCreator().getName())
                        .imageUrl(c.getImageUrl())
                        .build()
                )
                .collect(Collectors.toList());
    }

    // ✅ PHƯƠNG THỨC NÀY ĐƯỢC SỬ DỤNG CHO DANH SÁCH KHÓA HỌC ĐÃ MUA CÓ PHÂN TRANG
    public Page<Course> getCoursesByUserAndAccessType(UUID userId, CourseAccessType type, Pageable pageable) {
        if (type == CourseAccessType.PURCHASED) {
            // ✅ Sử dụng phương thức mới chỉ lấy các khóa học đã mua VÀ visible
            return courseManagementRepository.findByUserIdAndAccessTypeAndCourseVisibleTrue(userId, type, pageable)
                    .map(CourseManagement::getCourse);
        } else if (type == CourseAccessType.CREATED) {
            // Đối với khóa học do giáo viên tạo, họ phải luôn thấy tất cả khóa học của mình (visible hoặc ẩn)
            return courseRepository.findByCreatorId(userId, pageable);
        }
        // Đối với các loại truy cập khác (nếu có), bạn có thể thêm logic tương ứng
        // Mặc định, trả về rỗng hoặc ném ngoại lệ nếu loại không được hỗ trợ
        return Page.empty(pageable);
    }

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
}