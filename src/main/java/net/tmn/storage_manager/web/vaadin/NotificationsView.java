package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.Notification;
import net.tmn.storage_manager.database.jpa.type.NotificationStatus;
import net.tmn.storage_manager.database.jpa.type.NotificationType;
import net.tmn.storage_manager.service.NotificationDisplay;
import net.tmn.storage_manager.service.NotificationService;

@Route(value = "notifications", layout = MainLayout.class)
@PageTitle("Notifications")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationsView extends VerticalLayout {

    Grid<NotificationDisplay> grid = new Grid<>(NotificationDisplay.class, false);

    public NotificationsView(NotificationService notificationService) {
        setSizeFull();
        setPadding(true);
        configureGrid();
        List<NotificationDisplay> notifications = notificationService.getNotificationDisplays();
        add(createHeader(), createSummary(notifications), grid);
        grid.setItems(notifications);
    }

    private H2 createHeader() {
        H2 header = new H2("Notifications");
        header.getStyle().set("margin-top", "0");
        return header;
    }

    private HorizontalLayout createSummary(List<NotificationDisplay> notifications) {
        long pending = countByStatus(notifications, NotificationStatus.PENDING);
        long sent = countByStatus(notifications, NotificationStatus.SENT);
        long failed = countByStatus(notifications, NotificationStatus.FAILED);

        HorizontalLayout summary = new HorizontalLayout(
                summaryTile("Pending", pending, "contrast"),
                summaryTile("Sent", sent, "success"),
                summaryTile("Failed", failed, "error"));
        summary.setWidthFull();
        summary.getStyle().set("flex-wrap", "wrap");
        return summary;
    }

    private Div summaryTile(String label, long count, String theme) {
        Div tile = new Div();
        tile.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("padding", "var(--lumo-space-m)")
                .set("flex", "1 1 180px");
        Span number = VaadinViewUtils.badge(String.valueOf(count), theme);
        number.getStyle().set("font-size", "var(--lumo-font-size-xl)");
        Span text = new Span(label);
        text.getStyle().set("display", "block").set("margin-top", "var(--lumo-space-s)");
        tile.add(number, text);
        return tile;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setEmptyStateText("No notifications found.");
        grid.addComponentColumn(display -> typeBadge(display.notification().getNotificationType()))
                .setHeader("Notification Type")
                .setAutoWidth(true);
        grid.addColumn(display ->
                        VaadinViewUtils.enumLabel(display.notification().getTargetType()))
                .setHeader("Target Type")
                .setAutoWidth(true);
        grid.addColumn(NotificationDisplay::targetDisplay).setHeader("Target").setAutoWidth(true);
        grid.addColumn(display -> display.notification().getMessage())
                .setHeader("Message")
                .setFlexGrow(1);
        grid.addComponentColumn(display -> statusBadge(display.notification().getStatus()))
                .setHeader("Status")
                .setAutoWidth(true);
        grid.addColumn(display ->
                        VaadinViewUtils.formatDateTime(display.notification().getCreatedAt()))
                .setHeader("Created")
                .setAutoWidth(true);
        grid.addColumn(display ->
                        VaadinViewUtils.formatDateTime(display.notification().getSentAt()))
                .setHeader("Sent")
                .setAutoWidth(true);
    }

    private Component typeBadge(NotificationType type) {
        String theme =
                switch (type) {
                    case EXPIRY_WARNING -> "warning";
                    case EXPIRED -> "error";
                    case CUSTOM_REMINDER -> "primary";
                };
        return VaadinViewUtils.badge(VaadinViewUtils.enumLabel(type), theme);
    }

    private Component statusBadge(NotificationStatus status) {
        String theme =
                switch (status) {
                    case PENDING -> "contrast";
                    case SENT -> "success";
                    case FAILED -> "error";
                };
        return VaadinViewUtils.badge(VaadinViewUtils.enumLabel(status), theme);
    }

    private long countByStatus(List<NotificationDisplay> notifications, NotificationStatus status) {
        return notifications.stream()
                .map(NotificationDisplay::notification)
                .map(Notification::getStatus)
                .filter(status::equals)
                .count();
    }
}
