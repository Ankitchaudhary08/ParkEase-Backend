package com.parkease.notifanalytics.notification.repository;

import com.parkease.notifanalytics.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderBySentAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndIsRead(Long recipientId, boolean isRead);
    int countByRecipientIdAndIsRead(Long recipientId, boolean isRead);
    List<Notification> findByType(Notification.NotifType type);
    List<Notification> findByRelatedId(Long relatedId);
}
