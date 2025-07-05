package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.RatingDto;
import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.Rating;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final CourseRepository courseRepository;
    private final CourseManagementRepository courseManagementRepository;

    public RatingDto addOrUpdateRating(UUID courseId, int score, User user) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));

        boolean hasPurchased = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(user.getId(), courseId, CourseAccessType.PURCHASED)
                .isPresent();

        if (!hasPurchased) {
            throw new RuntimeException("Bạn chưa mua khóa học này");
        }

        // ✅ Nếu đã đánh giá → cập nhật
        Rating rating = ratingRepository.findByCourse_IdAndStudent_Id(courseId, user.getId())
                .map(existing -> {
                    existing.setValue(score);
                    return existing;
                })
                .orElseGet(() -> Rating.builder()
                        .student(user)
                        .course(course)
                        .value(score)
                        .build()
                );

        return RatingDto.from(ratingRepository.save(rating));
    }



    public Double getAverageRating(UUID courseId) {
        List<Rating> ratings = ratingRepository.findByCourse_Id(courseId);
        if (ratings.isEmpty()) return null;
        return ratings.stream().mapToInt(Rating::getValue).average().orElse(0.0);
    }

    public RatingDto getUserRating(UUID courseId, UUID userId) {
        return ratingRepository.findByCourse_IdAndStudent_Id(userId, courseId)
                .map(RatingDto::from).orElse(null);
    }
}
