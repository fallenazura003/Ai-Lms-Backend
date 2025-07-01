package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.LessonRequest;
import com.forsakenecho.learning_management_system.dto.LessonResponse;
import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.Lesson;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator; // Import này cần nếu sắp xếp thủ công
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    // Helper method để map Lesson entity sang LessonResponse DTO
    private LessonResponse mapToLessonResponse(Lesson lesson) {
        if (lesson == null) {
            return null;
        }
        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .youtubeVideoId(lesson.getYoutubeVideoId())
                .recallQuestion(lesson.getRecallQuestion())
                .material(lesson.getMaterial())
                .shortAnswer(lesson.getShortAnswer())
                .multipleChoice(lesson.getMultipleChoice())
                .summaryTask(lesson.getSummaryTask())
                .isRecallQuestionCompleted(lesson.isRecallQuestionCompleted())
                .isMaterialCompleted(lesson.isMaterialCompleted())
                .isShortAnswerCompleted(lesson.isShortAnswerCompleted())
                .isMultipleChoiceCompleted(lesson.isMultipleChoiceCompleted())
                .isSummaryTaskCompleted(lesson.isSummaryTaskCompleted())
                .isLessonCompleted(lesson.isLessonCompleted())
                .courseId(lesson.getCourse() != null ? lesson.getCourse().getId() : null)
                .lessonOrder(lesson.getLessonOrder()) // ✅ Ánh xạ lessonOrder
                .build();
    }

    public LessonResponse createLesson(UUID courseId, LessonRequest lessonRequest) {
        // 1. Tìm khóa học mà bài học này thuộc về
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với ID: " + courseId));

        // 2. Tạo đối tượng Lesson từ LessonRequest
        Lesson lesson = Lesson.builder()
                .title(lessonRequest.getTitle())
                .youtubeVideoId(lessonRequest.getYoutubeVideoId())
                .recallQuestion(lessonRequest.getRecallQuestion())
                .material(lessonRequest.getMaterial())
                .shortAnswer(lessonRequest.getShortAnswer())
                .multipleChoice(lessonRequest.getMultipleChoice())
                .summaryTask(lessonRequest.getSummaryTask())
                // Trạng thái hoàn thành mặc định là false khi tạo mới, không lấy từ request
                .isRecallQuestionCompleted(false)
                .isMaterialCompleted(false)
                .isShortAnswerCompleted(false)
                .isMultipleChoiceCompleted(false)
                .isSummaryTaskCompleted(false)
                .isLessonCompleted(false)
                .course(course) // Gán khóa học cho bài học
                .lessonOrder(lessonRequest.getLessonOrder()) // ✅ Gán lessonOrder từ request
                .build();



        // 3. Lưu bài học vào cơ sở dữ liệu
        Lesson savedLesson = lessonRepository.save(lesson);
        return mapToLessonResponse(savedLesson);
    }

    //  updateLesson
    public LessonResponse updateLesson(UUID courseId, UUID lessonId, LessonRequest lessonRequest) {
        // 1. Tìm khóa học để đảm bảo tồn tại và lesson thuộc về khóa học đó
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với ID: " + courseId));

        // 2. Tìm bài học cần cập nhật
        Lesson existingLesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        // 3. Kiểm tra xem bài học có thuộc về khóa học đã cho hay không
        if (!existingLesson.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài học không thuộc về khóa học này.");
        }

        // 4. Cập nhật các trường của bài học hiện có với dữ liệu từ request
        existingLesson.setTitle(lessonRequest.getTitle());
        existingLesson.setYoutubeVideoId(lessonRequest.getYoutubeVideoId());
        existingLesson.setRecallQuestion(lessonRequest.getRecallQuestion());
        existingLesson.setMaterial(lessonRequest.getMaterial());
        existingLesson.setShortAnswer(lessonRequest.getShortAnswer());
        existingLesson.setMultipleChoice(lessonRequest.getMultipleChoice());
        existingLesson.setSummaryTask(lessonRequest.getSummaryTask());

        // ✅ Cập nhật lessonOrder từ request
        existingLesson.setLessonOrder(lessonRequest.getLessonOrder());

        // ✅ Cập nhật các trường "isCompleted" từ request (nếu frontend có gửi)
        // Nếu các trường này không có trong LessonRequest hoặc bạn không muốn cập nhật từ form này,
        // hãy xem xét loại bỏ chúng hoặc tạo một DTO riêng cho việc cập nhật trạng thái.
        existingLesson.setRecallQuestionCompleted(lessonRequest.isRecallQuestionCompleted());
        existingLesson.setMaterialCompleted(lessonRequest.isMaterialCompleted());
        existingLesson.setShortAnswerCompleted(lessonRequest.isShortAnswerCompleted());
        existingLesson.setMultipleChoiceCompleted(lessonRequest.isMultipleChoiceCompleted());
        existingLesson.setSummaryTaskCompleted(lessonRequest.isSummaryTaskCompleted());
        existingLesson.setLessonCompleted(lessonRequest.isLessonCompleted());


        // 5. Lưu bài học đã cập nhật vào cơ sở dữ liệu
        Lesson updatedLesson = lessonRepository.save(existingLesson);
        return mapToLessonResponse(updatedLesson);
    }

    //  getLessonsByCourseId
    public List<LessonResponse> getLessonsByCourseId(UUID courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với ID: " + courseId));

        // ✅ Lấy danh sách bài học đã được sắp xếp theo lessonOrder
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonOrderAsc(courseId);

        // Chuyển đổi danh sách Lesson entities sang danh sách LessonResponse DTOs
        return lessons.stream()
                .map(this::mapToLessonResponse)
                .collect(Collectors.toList());
    }

    //  delete Lesson
    public void deleteLesson(UUID courseId, UUID lessonId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với ID: " + courseId));

        Lesson existingLesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        if (!existingLesson.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài học không thuộc về khóa học này.");
        }

        lessonRepository.delete(existingLesson);
    }

    // Phương thức lấy một bài học theo ID (có thể cần cho trang chi tiết bài học)
    public LessonResponse getLessonById(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));
        return mapToLessonResponse(lesson);
    }
}