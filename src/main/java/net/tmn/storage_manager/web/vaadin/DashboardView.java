package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.ItemInstance;
import net.tmn.storage_manager.service.ItemInstanceService;
import net.tmn.storage_manager.service.NotificationService;
import net.tmn.storage_manager.service.StorageBoxService;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardView extends VerticalLayout {

    ItemInstanceService itemInstanceService;
    StorageBoxService storageBoxService;
    NotificationService notificationService;

    public DashboardView(
            ItemInstanceService itemInstanceService,
            StorageBoxService storageBoxService,
            NotificationService notificationService) {
        this.itemInstanceService = itemInstanceService;
        this.storageBoxService = storageBoxService;
        this.notificationService = notificationService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        add(createHeader(), createDashboardContent());
    }

    private H2 createHeader() {
        H2 header = new H2("Dashboard");
        header.getStyle().set("margin-top", "0");
        return header;
    }

    private VerticalLayout createDashboardContent() {
        itemInstanceService.updateExpiredInstances();

        LocalDate today = LocalDate.now();
        List<ItemInstance> activeItems = itemInstanceService.getActiveItemInstances();
        List<ItemInstance> expiredItems = itemInstanceService.getExpiredItemInstances();
        List<ItemInstance> expiringItems =
                itemInstanceService.getItemInstancesExpiringBetween(today, today.plusDays(7));

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        content.add(createSummary(activeItems, expiredItems, expiringItems), createExpiredGrid(expiredItems));
        content.add(createExpiringGrid(expiringItems));
        return content;
    }

    private HorizontalLayout createSummary(
            List<ItemInstance> activeItems, List<ItemInstance> expiredItems, List<ItemInstance> expiringItems) {
        HorizontalLayout summary = new HorizontalLayout(
                summaryTile("Active Items", activeItems.size(), "primary"),
                summaryTile("Expired Items", expiredItems.size(), "error"),
                summaryTile("Expiring Soon", expiringItems.size(), "warning"),
                summaryTile(
                        "Storage Boxes", storageBoxService.getAllStorageBoxes().size(), "contrast"),
                summaryTile(
                        "Pending Notifications",
                        notificationService.getPendingNotifications().size(),
                        "success"));
        summary.setWidthFull();
        summary.setSpacing(true);
        summary.getStyle().set("flex-wrap", "wrap");
        return summary;
    }

    private Div summaryTile(String label, int count, String theme) {
        Div tile = new Div();
        tile.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("padding", "var(--lumo-space-m)")
                .set("flex", "1 1 190px")
                .set("min-width", "170px");

        Span number = VaadinViewUtils.badge(String.valueOf(count), theme);
        number.getStyle().set("font-size", "var(--lumo-font-size-xl)");
        Span text = new Span(label);
        text.getStyle().set("display", "block").set("margin-top", "var(--lumo-space-s)");
        tile.add(number, text);
        return tile;
    }

    private Div createExpiredGrid(List<ItemInstance> expiredItems) {
        Grid<ItemInstance> grid = baseItemGrid("No expired item instances.");
        grid.addColumn(item -> ChronoUnit.DAYS.between(item.getBestBeforeDate(), LocalDate.now()))
                .setHeader("Days Expired")
                .setAutoWidth(true);
        grid.setItems(expiredItems);
        return section("Expired Items", grid);
    }

    private Div createExpiringGrid(List<ItemInstance> expiringItems) {
        Grid<ItemInstance> grid = baseItemGrid("No item instances expiring soon.");
        grid.addColumn(item -> ChronoUnit.DAYS.between(LocalDate.now(), item.getBestBeforeDate()))
                .setHeader("Days Remaining")
                .setAutoWidth(true);
        grid.setItems(expiringItems);
        return section("Items Expiring Soon", grid);
    }

    private Grid<ItemInstance> baseItemGrid(String emptyText) {
        Grid<ItemInstance> grid = new Grid<>(ItemInstance.class, false);
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText(emptyText);
        grid.addColumn(ItemInstance::getTitle)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(VaadinViewUtils::itemTypeName).setHeader("Item Type").setAutoWidth(true);
        grid.addColumn(item -> VaadinViewUtils.formatDate(item.getBestBeforeDate()))
                .setHeader("Best Before")
                .setAutoWidth(true);
        return grid;
    }

    private Div section(String title, Grid<ItemInstance> grid) {
        Div section = new Div();
        section.setWidthFull();
        H2 heading = new H2(title);
        heading.getStyle().set("font-size", "var(--lumo-font-size-l)");
        section.add(heading, grid);
        return section;
    }
}
