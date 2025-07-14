package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.entity.Notification;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import com.forsakenecho.learning_management_system.enums.NotificationType;
import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import com.forsakenecho.learning_management_system.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final CourseManagementRepository courseManagementRepository; // ✅ thêm

    public void sendNotification(String recipientEmail, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setRecipientEmail(recipientEmail);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notificationRepo.save(notification);

        // Gửi qua WebSocket
        messagingTemplate.convertAndSendToUser(
                recipientEmail,
                "/queue/notifications",
                notification
        );
    }

    /**
     * ✅ Gửi thông báo đến tất cả học sinh đã mua một khóa học
     */
    public void sendNotificationToStudentsOfCourse(UUID courseId, String message, NotificationType type) {
        List<CourseManagement> purchased = courseManagementRepository
                .findByCourseIdAndAccessType(courseId, CourseAccessType.PURCHASED);

        for (CourseManagement cm : purchased) {
            String email = cm.getUser().getEmail();
            sendNotification(email, message, type);
        }
    }

    public List<Notification> getNotificationsForUser(String email) {
        return notificationRepo.findByRecipientEmailOrderByCreatedAtDesc(email);
    }

    public void markAsRead(UUID id) {
        notificationRepo.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepo.save(n);
        });
    }
}
