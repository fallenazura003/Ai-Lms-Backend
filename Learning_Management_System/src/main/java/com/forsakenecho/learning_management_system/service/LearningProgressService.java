package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.LearningProgress;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.LearningProgressRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LearningProgressService {
    private final LearningProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public LearningProgress completeLesson(UUID studentId, UUID courseId, UUID lessonId) {
        LearningProgress progress = progressRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseGet(() -> {
                    Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new RuntimeException("Course not found"));
                    User student = userRepository.findById(studentId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return LearningProgress.builder()
                            .student(student)
                            .course(course)
                            .totalLessons(course.getLessons().size())
                            .completedLessons(0)
                            .status(LearningProgress.ProgressStatus.IN_PROGRESS)
                            .lastAccessedAt(LocalDateTime.now())
                            .build();
                });

        if (progress.getCompletedLessonIds().add(lessonId)) {
            progress.setCompletedLessons(progress.getCompletedLessonIds().size());
            progress.setLastAccessedAt(LocalDateTime.now());
            if (progress.getCompletedLessons() >= progress.getTotalLessons()) {
                progress.setStatus(LearningProgress.ProgressStatus.COMPLETED);
            }
        }

        return progressRepository.save(progress);
    }

    public List<LearningProgress> getProgressForStudent(UUID studentId) {
        return progressRepository.findByStudentId(studentId);
    }

    // ✅ Phương thức mới: Lấy tiến độ của một khóa học cụ thể
    public Optional<LearningProgress> getProgressForStudentAndCourse(UUID studentId, UUID courseId) {
        return progressRepository.findByStudentIdAndCourseId(studentId, courseId);
    }
}
