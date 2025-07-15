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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional; // Import this

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final CourseManagementRepository courseManagementRepository;

    public void sendNotification(String recipientEmail, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setRecipientEmail(recipientEmail);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false); // Make sure this is setIsRead
        notificationRepo.save(notification);

        // Gửi qua WebSocket
        messagingTemplate.convertAndSendToUser(
                recipientEmail,
                "/queue/notifications",
                notification
        );
    }

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

    @Transactional
    public void markAsRead(UUID id) {
        notificationRepo.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepo.save(n); // Explicitly save the single notification
        });
    }

    @Transactional
    public void markAllAsReadForUser(String email) {
        List<Notification> unreadNotifications = notificationRepo.findByRecipientEmailAndIsReadFalse(email);
        if (!unreadNotifications.isEmpty()) { // Only proceed if there are unread notifications
            for (Notification n : unreadNotifications) {
                n.setRead(true); // Update the entity
            }
            notificationRepo.saveAll(unreadNotifications); // Save all updated entities
            // Optionally, add a log here to confirm saving
            System.out.println("Marked " + unreadNotifications.size() + " notifications as read for user: " + email);
        } else {
            System.out.println("No unread notifications found for user: " + email);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        notificationRepo.deleteByCreatedAtBefore(sevenDaysAgo);
        System.out.println("Scheduled task: Deleted notifications older than 7 days.");
    }
}