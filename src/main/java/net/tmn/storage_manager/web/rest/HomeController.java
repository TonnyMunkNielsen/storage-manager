package net.tmn.storage_manager.web.rest;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.service.NotificationService;
import net.tmn.storage_manager.service.ProduceInstanceService;
import net.tmn.storage_manager.service.StorageBoxService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HomeController {

    ProduceInstanceService produceInstanceService;
    NotificationService notificationService;
    StorageBoxService storageBoxService;

    @GetMapping("/")
    public String home(Model model) {
        // Get dashboard data
        model.addAttribute("activeProduces", produceInstanceService.getActiveProduceInstances());
        model.addAttribute("expiredProduces", produceInstanceService.getExpiredProduceInstances());
        model.addAttribute(
                "expiringProduces",
                produceInstanceService.getProduceInstancesExpiringBetween(
                        LocalDate.now(), LocalDate.now().plusDays(7)));
        model.addAttribute("storageBoxes", storageBoxService.getAllStorageBoxes());
        model.addAttribute("notifications", notificationService.getPendingNotifications());

        return "home";
    }
}
