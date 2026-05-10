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
import net.tmn.storage_manager.database.jpa.ProduceInstance;
import net.tmn.storage_manager.service.NotificationService;
import net.tmn.storage_manager.service.ProduceInstanceService;
import net.tmn.storage_manager.service.StorageBoxService;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardView extends VerticalLayout {

    ProduceInstanceService produceInstanceService;
    StorageBoxService storageBoxService;
    NotificationService notificationService;

    public DashboardView(
            ProduceInstanceService produceInstanceService,
            StorageBoxService storageBoxService,
            NotificationService notificationService) {
        this.produceInstanceService = produceInstanceService;
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
        LocalDate today = LocalDate.now();
        List<ProduceInstance> activeProduces = produceInstanceService.getActiveProduceInstances();
        List<ProduceInstance> expiredProduces = produceInstanceService.getExpiredProduceInstances();
        List<ProduceInstance> expiringProduces =
                produceInstanceService.getProduceInstancesExpiringBetween(today, today.plusDays(7));

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        content.add(
                createSummary(activeProduces, expiredProduces, expiringProduces), createExpiredGrid(expiredProduces));
        content.add(createExpiringGrid(expiringProduces));
        return content;
    }

    private HorizontalLayout createSummary(
            List<ProduceInstance> activeProduces,
            List<ProduceInstance> expiredProduces,
            List<ProduceInstance> expiringProduces) {
        HorizontalLayout summary = new HorizontalLayout(
                summaryTile("Active Produces", activeProduces.size(), "primary"),
                summaryTile("Expired Produces", expiredProduces.size(), "error"),
                summaryTile("Expiring Soon", expiringProduces.size(), "warning"),
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

    private Div createExpiredGrid(List<ProduceInstance> expiredProduces) {
        Grid<ProduceInstance> grid = baseProduceGrid("No expired produce instances.");
        grid.addColumn(produce -> ChronoUnit.DAYS.between(produce.getBestBeforeDate(), LocalDate.now()))
                .setHeader("Days Expired")
                .setAutoWidth(true);
        grid.setItems(expiredProduces);
        return section("Expired Produces", grid);
    }

    private Div createExpiringGrid(List<ProduceInstance> expiringProduces) {
        Grid<ProduceInstance> grid = baseProduceGrid("No produce instances expiring soon.");
        grid.addColumn(produce -> ChronoUnit.DAYS.between(LocalDate.now(), produce.getBestBeforeDate()))
                .setHeader("Days Remaining")
                .setAutoWidth(true);
        grid.setItems(expiringProduces);
        return section("Produces Expiring Soon", grid);
    }

    private Grid<ProduceInstance> baseProduceGrid(String emptyText) {
        Grid<ProduceInstance> grid = new Grid<>(ProduceInstance.class, false);
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText(emptyText);
        grid.addColumn(ProduceInstance::getTitle)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(VaadinViewUtils::produceTypeName)
                .setHeader("Produce Type")
                .setAutoWidth(true);
        grid.addColumn(produce -> VaadinViewUtils.formatDate(produce.getBestBeforeDate()))
                .setHeader("Best Before")
                .setAutoWidth(true);
        return grid;
    }

    private Div section(String title, Grid<ProduceInstance> grid) {
        Div section = new Div();
        section.setWidthFull();
        H2 heading = new H2(title);
        heading.getStyle().set("font-size", "var(--lumo-font-size-l)");
        section.add(heading, grid);
        return section;
    }
}
