package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.ProduceInstance;
import net.tmn.storage_manager.database.jpa.ProduceType;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.jpa.type.ProduceInstanceStatus;
import net.tmn.storage_manager.service.ProduceInstanceService;
import net.tmn.storage_manager.service.ProduceTypeService;
import net.tmn.storage_manager.service.StorageBoxService;

@Route(value = "produce-instances", layout = MainLayout.class)
@PageTitle("Produce Instances")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProduceInstancesView extends VerticalLayout {

    final ProduceInstanceService produceInstanceService;
    final ProduceTypeService produceTypeService;
    final StorageBoxService storageBoxService;
    final Grid<ProduceInstance> grid = new Grid<>(ProduceInstance.class, false);
    final Binder<ProduceInstance> binder = new Binder<>(ProduceInstance.class);
    final Dialog dialog = new Dialog();
    final ComboBox<ProduceType> produceType = new ComboBox<>("Produce type");
    final TextField title = new TextField("Title");
    final DatePicker bestBeforeDate = new DatePicker("Best-before date");
    final ComboBox<StorageBox> storageBox = new ComboBox<>("Storage box");
    final ComboBox<ProduceInstanceStatus> status = new ComboBox<>("Status");
    final Div produceTypePreview = new Div();

    ProduceInstance editedInstance;
    List<ProduceType> produceTypes = List.of();
    List<StorageBox> storageBoxes = List.of();

    public ProduceInstancesView(
            ProduceInstanceService produceInstanceService,
            ProduceTypeService produceTypeService,
            StorageBoxService storageBoxService) {
        this.produceInstanceService = produceInstanceService;
        this.produceTypeService = produceTypeService;
        this.storageBoxService = storageBoxService;

        setSizeFull();
        setPadding(true);
        configureGrid();
        configureBinder();
        add(createHeader(), grid);
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H2 header = new H2("Produce Instances");
        header.getStyle().set("margin", "0");

        Button create = VaadinViewUtils.primaryButton("Add Produce Instance", VaadinIcon.PLUS);
        create.addClickListener(event -> openEditor(newProduceInstance()));

        HorizontalLayout layout = new HorizontalLayout(header, create);
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        layout.getStyle().set("flex-wrap", "wrap");
        return layout;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setEmptyStateText("No produce instances found.");
        grid.addColumn(ProduceInstance::getTitle)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(VaadinViewUtils::produceTypeName)
                .setHeader("Produce Type")
                .setAutoWidth(true);
        grid.addColumn(VaadinViewUtils::storageBoxNumber)
                .setHeader("Storage Box")
                .setAutoWidth(true);
        grid.addColumn(instance -> VaadinViewUtils.formatDate(instance.getBestBeforeDate()))
                .setHeader("Best Before")
                .setAutoWidth(true);
        grid.addComponentColumn(instance -> statusBadge(instance.getStatus()))
                .setHeader("Status")
                .setAutoWidth(true);
        grid.addComponentColumn(this::daysRemainingBadge)
                .setHeader("Days Remaining")
                .setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("Actions").setAutoWidth(true);
    }

    private HorizontalLayout actions(ProduceInstance instance) {
        Button edit = VaadinViewUtils.iconButton(VaadinIcon.EDIT, "Edit");
        edit.addClickListener(event -> openEditor(instance));

        Button delete = VaadinViewUtils.iconButton(VaadinIcon.TRASH, "Delete");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> confirmDelete(instance));

        return new HorizontalLayout(edit, delete);
    }

    private void configureBinder() {
        produceType.setItemLabelGenerator(ProduceType::getName);
        produceType.setRequiredIndicatorVisible(true);
        produceType.addValueChangeListener(event -> updateProduceTypePreview(event.getValue()));

        title.setRequiredIndicatorVisible(true);
        storageBox.setItemLabelGenerator(box -> "Box #" + box.getBoxNumber());
        storageBox.setRequiredIndicatorVisible(true);
        status.setItems(ProduceInstanceStatus.values());
        status.setItemLabelGenerator(VaadinViewUtils::enumLabel);

        binder.forField(produceType)
                .asRequired("Produce type is required")
                .bind(ProduceInstance::getProduceType, ProduceInstance::setProduceType);
        binder.forField(title)
                .asRequired("Title is required")
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Title is required")
                .bind(ProduceInstance::getTitle, ProduceInstance::setTitle);
        binder.forField(bestBeforeDate)
                .asRequired("Best-before date is required")
                .bind(ProduceInstance::getBestBeforeDate, ProduceInstance::setBestBeforeDate);
        binder.forField(storageBox)
                .asRequired("Storage box is required")
                .bind(ProduceInstance::getStorageBox, ProduceInstance::setStorageBox);
        binder.forField(status)
                .asRequired("Status is required")
                .bind(ProduceInstance::getStatus, ProduceInstance::setStatus);
    }

    private ProduceInstance newProduceInstance() {
        ProduceInstance instance = new ProduceInstance();
        instance.setBestBeforeDate(LocalDate.now());
        instance.setStatus(ProduceInstanceStatus.ACTIVE);
        return instance;
    }

    private void openEditor(ProduceInstance instance) {
        editedInstance = instance;
        boolean createMode = instance.getId() == null;
        produceTypes = produceTypeService.getAllProduceTypes();
        storageBoxes = storageBoxService.getAllStorageBoxes();
        produceType.setItems(produceTypes);
        storageBox.setItems(storageBoxes);

        dialog.removeAll();
        dialog.setHeaderTitle(createMode ? "Add Produce Instance" : "Edit Produce Instance");
        produceType.setReadOnly(!createMode);
        status.setVisible(!createMode);

        binder.readBean(editedInstance);
        selectMatchingValues();
        updateProduceTypePreview(produceType.getValue());

        FormLayout form = new FormLayout(produceType, title, bestBeforeDate, storageBox, status, produceTypePreview);
        form.setColspan(produceTypePreview, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("680px", 2));

        Button cancel = new Button("Cancel", event -> dialog.close());
        Button save = VaadinViewUtils.primaryButton("Save", VaadinIcon.CHECK);
        save.addClickListener(event -> save());

        HorizontalLayout buttons = new HorizontalLayout(cancel, save);
        buttons.setJustifyContentMode(JustifyContentMode.END);
        buttons.setWidthFull();

        dialog.add(new VerticalLayout(form, buttons));
        dialog.open();
    }

    private void selectMatchingValues() {
        if (editedInstance.getProduceType() != null) {
            findMatchingProduceType(editedInstance.getProduceType()).ifPresent(produceType::setValue);
        }

        if (editedInstance.getStorageBox() != null) {
            findMatchingStorageBox(editedInstance.getStorageBox()).ifPresent(storageBox::setValue);
        }
    }

    private java.util.Optional<ProduceType> findMatchingProduceType(ProduceType selected) {
        return produceTypes.stream()
                .filter(type -> Objects.equals(type.getId(), selected.getId()))
                .findFirst();
    }

    private java.util.Optional<StorageBox> findMatchingStorageBox(StorageBox selected) {
        return storageBoxes.stream()
                .filter(box -> Objects.equals(box.getId(), selected.getId()))
                .findFirst();
    }

    private void updateProduceTypePreview(ProduceType selectedProduceType) {
        produceTypePreview.removeAll();
        produceTypePreview
                .getStyle()
                .set("min-height", "180px")
                .set("border", "1px dashed var(--lumo-contrast-30pct)")
                .set("border-radius", "8px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "var(--lumo-space-m)");

        if (VaadinViewUtils.hasImage(selectedProduceType)) {
            Image image = new Image(VaadinViewUtils.imageUrl(selectedProduceType), selectedProduceType.getName());
            image.setMaxWidth("100%");
            image.setMaxHeight("220px");
            image.getStyle().set("object-fit", "contain").set("border-radius", "6px");
            produceTypePreview.add(image);
            return;
        }

        produceTypePreview.add(VaadinViewUtils.emptyText("No image available."));
    }

    private void save() {
        try {
            binder.writeBean(editedInstance);
            editedInstance.setTitle(editedInstance.getTitle().trim());

            if (editedInstance.getId() == null) {
                produceInstanceService.createProduceInstance(editedInstance);
                VaadinViewUtils.success("Produce instance created.");
            } else {
                produceInstanceService.updateProduceInstance(editedInstance.getId(), editedInstance);
                VaadinViewUtils.success("Produce instance updated.");
            }
            dialog.close();
            refreshGrid();
        } catch (ValidationException e) {
            VaadinViewUtils.error("Check the highlighted fields.");
        } catch (RuntimeException e) {
            VaadinViewUtils.error(e.getMessage());
        }
    }

    private void confirmDelete(ProduceInstance instance) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete produce instance");
        confirm.setText("Delete " + instance.getTitle() + "?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(event -> {
            try {
                produceInstanceService.deleteProduceInstance(instance.getId());
                VaadinViewUtils.success("Produce instance deleted.");
                refreshGrid();
            } catch (RuntimeException e) {
                VaadinViewUtils.error(e.getMessage());
            }
        });
        confirm.open();
    }

    private Component statusBadge(ProduceInstanceStatus status) {
        String theme =
                switch (status) {
                    case ACTIVE -> "success";
                    case EXPIRED -> "error";
                    case REPLACED -> "warning";
                    case CONSUMED -> "contrast";
                };
        return VaadinViewUtils.badge(VaadinViewUtils.enumLabel(status), theme);
    }

    private Component daysRemainingBadge(ProduceInstance instance) {
        if (instance.getBestBeforeDate() == null) {
            return VaadinViewUtils.emptyText("-");
        }

        long daysRemaining = daysRemaining(instance);
        if (daysRemaining < 0) {
            return VaadinViewUtils.badge("Expired " + Math.abs(daysRemaining) + " days ago", "error");
        }
        if (daysRemaining <= 7) {
            return VaadinViewUtils.badge(daysRemaining + " days", "warning");
        }
        return VaadinViewUtils.badge(daysRemaining + " days", "success");
    }

    private long daysRemaining(ProduceInstance instance) {
        int notificationDaysModifier =
                instance.getProduceType() == null || instance.getProduceType().getNotificationDaysModifier() == null
                        ? 0
                        : instance.getProduceType().getNotificationDaysModifier();
        return ChronoUnit.DAYS.between(
                LocalDate.now(), instance.getBestBeforeDate().plusDays(notificationDaysModifier));
    }

    private void refreshGrid() {
        grid.setItems(produceInstanceService.getAllProduceInstances());
    }
}
