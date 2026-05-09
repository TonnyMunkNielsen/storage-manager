package net.tmn.storage_manager.web.rest;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.Notification;
import net.tmn.storage_manager.database.jpa.type.NotificationStatus;
import net.tmn.storage_manager.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationService notificationService;

    @GetMapping
    public String listNotifications(Model model) {
        List<Notification> notifications = notificationService.getAllNotifications();

        // Calculate counts
        long pendingCount = notifications.stream()
                .filter(n -> n.getStatus() == NotificationStatus.PENDING)
                .count();
        long sentCount = notifications.stream()
                .filter(n -> n.getStatus() == NotificationStatus.SENT)
                .count();
        long failedCount = notifications.stream()
                .filter(n -> n.getStatus() == NotificationStatus.FAILED)
                .count();

        model.addAttribute("notifications", notifications);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("failedCount", failedCount);

        return "notifications/list";
    }
}
