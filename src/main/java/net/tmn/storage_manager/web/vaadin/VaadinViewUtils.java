package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.tmn.storage_manager.database.jpa.ProduceInstance;
import net.tmn.storage_manager.database.jpa.ProduceType;
import net.tmn.storage_manager.database.jpa.StorageBox;

final class VaadinViewUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private VaadinViewUtils() {}

    static Button primaryButton(String text, VaadinIcon icon) {
        Button button = new Button(text, icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    static Button iconButton(VaadinIcon icon, String tooltip) {
        Button button = new Button(icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.setTooltipText(tooltip);
        return button;
    }

    static Span badge(String text, String theme) {
        Span badge = new Span(text);
        badge.getElement().getThemeList().add("badge " + theme);
        return badge;
    }

    static void success(String message) {
        show(message, NotificationVariant.LUMO_SUCCESS);
    }

    static void error(String message) {
        show(message, NotificationVariant.LUMO_ERROR);
    }

    static String enumLabel(Enum<?> value) {
        if (value == null) {
            return "";
        }

        String[] words = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.toString();
    }

    static String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMATTER.format(dateTime);
    }

    static String formatMoney(BigDecimal amount) {
        return amount == null ? "-" : amount.stripTrailingZeros().toPlainString() + " kr.";
    }

    static String produceTypeName(ProduceInstance produceInstance) {
        ProduceType produceType = produceInstance.getProduceType();
        return produceType == null ? "-" : produceType.getName();
    }

    static String storageBoxNumber(ProduceInstance produceInstance) {
        StorageBox storageBox = produceInstance.getStorageBox();
        return storageBox == null ? "-" : String.valueOf(storageBox.getBoxNumber());
    }

    static boolean hasImage(ProduceType produceType) {
        return produceType != null
                && produceType.getId() != null
                && produceType.getImageData() != null
                && produceType.getImageData().length > 0;
    }

    static String imageUrl(ProduceType produceType) {
        return "/api/images/produce-type/" + produceType.getId();
    }

    static Image thumbnail(ProduceType produceType) {
        Image image = new Image(imageUrl(produceType), produceType.getName());
        image.setWidth("52px");
        image.setHeight("52px");
        image.getStyle()
                .set("object-fit", "cover")
                .set("border-radius", "6px")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("cursor", "pointer");
        return image;
    }

    static Component emptyText(String text) {
        Span span = new Span(text);
        span.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return span;
    }

    private static void show(String message, NotificationVariant variant) {
        com.vaadin.flow.component.notification.Notification notification =
                com.vaadin.flow.component.notification.Notification.show(message, 3500, Position.TOP_END);
        notification.addThemeVariants(variant);
    }
}
