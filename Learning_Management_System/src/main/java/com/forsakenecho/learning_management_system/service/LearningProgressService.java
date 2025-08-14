package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.CourseProgressSummaryDTO;
import com.forsakenecho.learning_management_system.dto.LearningProgressDTO;
import com.forsakenecho.learning_management_system.dto.LessonProgressDTO;
import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.LearningProgress;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.LearningProgressRepository;
import com.forsakenecho.learning_management_system.repository.LessonRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningProgressService {

    private final LearningProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    private LearningProgress getOrCreate(UUID studentId, UUID courseId) {
        return progressRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseGet(() -> {
                    Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new NoSuchElementException("Course not found"));
                    User student = userRepository.findById(studentId)
                            .orElseThrow(() -> new NoSuchElementException("User not found"));

                    int totalLessons = lessonRepository.countByCourseId(courseId);

                    return LearningProgress.builder()
                            .student(student)
                            .course(course)
                            .totalLessons(totalLessons)
                            .status(LearningProgress.ProgressStatus.IN_PROGRESS)
                            .lastAccessedAt(LocalDateTime.now())
                            .build();
                });
    }

    @Transactional
    public LearningProgress completeLesson(UUID studentId, UUID courseId, UUID lessonId) {
        LearningProgress progress = getOrCreate(studentId, courseId);
        if (progress.getCompletedLessonIds().add(lessonId)) {
            if (progress.getTotalLessons() == 0) {
                progress.setTotalLessons(lessonRepository.countByCourseId(courseId));
            }
            progress.setLastAccessedAt(LocalDateTime.now());
        }
        return progressRepository.save(progress);
    }

    @Transactional
    public LearningProgress uncompleteLesson(UUID studentId, UUID courseId, UUID lessonId) {
        LearningProgress progress = getOrCreate(studentId, courseId);
        if (progress.getCompletedLessonIds().remove(lessonId)) {
            if (progress.getTotalLessons() == 0) {
                progress.setTotalLessons(lessonRepository.countByCourseId(courseId));
            }
            progress.setLastAccessedAt(LocalDateTime.now());
        }
        return progressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public List<LearningProgressDTO> getProgressForStudent(UUID studentId) {
        return progressRepository.findByStudentId(studentId).stream()
                .map(LearningProgressDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<LearningProgressDTO> getProgressForStudentAndCourse(UUID studentId, UUID courseId) {
        return progressRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(LearningProgressDTO::from);
    }

    @Transactional(readOnly = true)
    public Optional<CourseProgressSummaryDTO> getSummary(UUID studentId, UUID courseId) {
        return progressRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(CourseProgressSummaryDTO::from);
    }

    @Transactional(readOnly = true)
    public List<LessonProgressDTO> getUserCourseProgress(UUID userId, UUID courseId) {
        LearningProgress lp = progressRepository.findByStudentIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new NoSuchElementException("Progress not found"));

        List<UUID> lessonIds = lessonRepository.findLessonIdsByCourseId(courseId);
        Set<UUID> completedIds = lp.getCompletedLessonIds();

        return lessonIds.stream()
                .map(id -> new LessonProgressDTO(id, completedIds.contains(id)))
                .collect(Collectors.toList());
    }
}
