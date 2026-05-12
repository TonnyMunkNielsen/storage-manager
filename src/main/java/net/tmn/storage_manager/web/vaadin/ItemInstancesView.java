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
import net.tmn.storage_manager.database.jpa.ItemInstance;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.jpa.type.ItemInstanceStatus;
import net.tmn.storage_manager.service.ItemInstanceService;
import net.tmn.storage_manager.service.ItemTypeService;
import net.tmn.storage_manager.service.StorageBoxService;

@Route(value = "item-instances", layout = MainLayout.class)
@PageTitle("Item Instances")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemInstancesView extends VerticalLayout {

    final ItemInstanceService itemInstanceService;
    final ItemTypeService itemTypeService;
    final StorageBoxService storageBoxService;
    final Grid<ItemInstance> grid = new Grid<>(ItemInstance.class, false);
    final Binder<ItemInstance> binder = new Binder<>(ItemInstance.class);
    final Dialog dialog = new Dialog();
    final ComboBox<ItemType> itemType = new ComboBox<>("Item type");
    final TextField title = new TextField("Title");
    final DatePicker bestBeforeDate = new DatePicker("Best-before date");
    final ComboBox<StorageBox> storageBox = new ComboBox<>("Storage box");
    final ComboBox<ItemInstanceStatus> status = new ComboBox<>("Status");
    final Div itemTypePreview = new Div();

    ItemInstance editedInstance;
    List<ItemType> itemTypes = List.of();
    List<StorageBox> storageBoxes = List.of();

    public ItemInstancesView(
            ItemInstanceService itemInstanceService,
            ItemTypeService itemTypeService,
            StorageBoxService storageBoxService) {
        this.itemInstanceService = itemInstanceService;
        this.itemTypeService = itemTypeService;
        this.storageBoxService = storageBoxService;

        setSizeFull();
        setPadding(true);
        configureGrid();
        configureBinder();
        add(createHeader(), grid);
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H2 header = new H2("Item Instances");
        header.getStyle().set("margin", "0");

        Button create = VaadinViewUtils.primaryButton("Add Item Instance", VaadinIcon.PLUS);
        create.addClickListener(event -> openEditor(newItemInstance()));

        HorizontalLayout layout = new HorizontalLayout(header, create);
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        layout.getStyle().set("flex-wrap", "wrap");
        return layout;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setEmptyStateText("No item instances found.");
        grid.addColumn(ItemInstance::getTitle)
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(VaadinViewUtils::itemTypeName).setHeader("Item Type").setAutoWidth(true);
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

    private HorizontalLayout actions(ItemInstance instance) {
        Button edit = VaadinViewUtils.iconButton(VaadinIcon.EDIT, "Edit");
        edit.addClickListener(event -> openEditor(instance));

        Button delete = VaadinViewUtils.iconButton(VaadinIcon.TRASH, "Delete");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> confirmDelete(instance));

        return new HorizontalLayout(edit, delete);
    }

    private void configureBinder() {
        itemType.setItemLabelGenerator(ItemType::getName);
        itemType.setRequiredIndicatorVisible(true);
        itemType.addValueChangeListener(event -> updateItemTypePreview(event.getValue()));

        title.setRequiredIndicatorVisible(true);
        storageBox.setItemLabelGenerator(box -> "Box #" + box.getBoxNumber());
        storageBox.setRequiredIndicatorVisible(true);
        status.setItems(ItemInstanceStatus.values());
        status.setItemLabelGenerator(VaadinViewUtils::enumLabel);

        binder.forField(itemType)
                .asRequired("Item type is required")
                .bind(ItemInstance::getItemType, ItemInstance::setItemType);
        binder.forField(title)
                .asRequired("Title is required")
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Title is required")
                .bind(ItemInstance::getTitle, ItemInstance::setTitle);
        binder.forField(bestBeforeDate)
                .asRequired("Best-before date is required")
                .bind(ItemInstance::getBestBeforeDate, ItemInstance::setBestBeforeDate);
        binder.forField(storageBox)
                .asRequired("Storage box is required")
                .bind(ItemInstance::getStorageBox, ItemInstance::setStorageBox);
        binder.forField(status).asRequired("Status is required").bind(ItemInstance::getStatus, ItemInstance::setStatus);
    }

    private ItemInstance newItemInstance() {
        ItemInstance instance = new ItemInstance();
        instance.setBestBeforeDate(LocalDate.now());
        instance.setStatus(ItemInstanceStatus.ACTIVE);
        return instance;
    }

    private void openEditor(ItemInstance instance) {
        editedInstance = instance;
        boolean createMode = instance.getId() == null;
        itemTypes = itemTypeService.getAllItemTypes();
        storageBoxes = storageBoxService.getAllStorageBoxes();
        itemType.setItems(itemTypes);
        storageBox.setItems(storageBoxes);

        dialog.removeAll();
        dialog.setHeaderTitle(createMode ? "Add Item Instance" : "Edit Item Instance");
        itemType.setReadOnly(!createMode);
        status.setVisible(!createMode);

        binder.readBean(editedInstance);
        selectMatchingValues();
        updateItemTypePreview(itemType.getValue());

        FormLayout form = new FormLayout(itemType, title, bestBeforeDate, storageBox, status, itemTypePreview);
        form.setColspan(itemTypePreview, 2);
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
        if (editedInstance.getItemType() != null) {
            findMatchingItemType(editedInstance.getItemType()).ifPresent(itemType::setValue);
        }

        if (editedInstance.getStorageBox() != null) {
            findMatchingStorageBox(editedInstance.getStorageBox()).ifPresent(storageBox::setValue);
        }
    }

    private java.util.Optional<ItemType> findMatchingItemType(ItemType selected) {
        return itemTypes.stream()
                .filter(type -> Objects.equals(type.getId(), selected.getId()))
                .findFirst();
    }

    private java.util.Optional<StorageBox> findMatchingStorageBox(StorageBox selected) {
        return storageBoxes.stream()
                .filter(box -> Objects.equals(box.getId(), selected.getId()))
                .findFirst();
    }

    private void updateItemTypePreview(ItemType selectedItemType) {
        itemTypePreview.removeAll();
        itemTypePreview
                .getStyle()
                .set("min-height", "180px")
                .set("border", "1px dashed var(--lumo-contrast-30pct)")
                .set("border-radius", "8px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "var(--lumo-space-m)");

        if (VaadinViewUtils.hasImage(selectedItemType)) {
            Image image = new Image(VaadinViewUtils.imageUrl(selectedItemType), selectedItemType.getName());
            image.setMaxWidth("100%");
            image.setMaxHeight("220px");
            image.getStyle().set("object-fit", "contain").set("border-radius", "6px");
            itemTypePreview.add(image);
            return;
        }

        itemTypePreview.add(VaadinViewUtils.emptyText("No image available."));
    }

    private void save() {
        try {
            binder.writeBean(editedInstance);
            editedInstance.setTitle(editedInstance.getTitle().trim());

            if (editedInstance.getId() == null) {
                itemInstanceService.createItemInstance(editedInstance);
                VaadinViewUtils.success("Item instance created.");
            } else {
                itemInstanceService.updateItemInstance(editedInstance.getId(), editedInstance);
                VaadinViewUtils.success("Item instance updated.");
            }
            dialog.close();
            refreshGrid();
        } catch (ValidationException e) {
            VaadinViewUtils.error("Check the highlighted fields.");
        } catch (RuntimeException e) {
            VaadinViewUtils.error(e.getMessage());
        }
    }

    private void confirmDelete(ItemInstance instance) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete item instance");
        confirm.setText("Delete " + instance.getTitle() + "?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(event -> {
            try {
                itemInstanceService.deleteItemInstance(instance.getId());
                VaadinViewUtils.success("Item instance deleted.");
                refreshGrid();
            } catch (RuntimeException e) {
                VaadinViewUtils.error(e.getMessage());
            }
        });
        confirm.open();
    }

    private Component statusBadge(ItemInstanceStatus status) {
        String theme =
                switch (status) {
                    case ACTIVE -> "success";
                    case EXPIRED -> "error";
                    case REPLACED -> "warning";
                    case CONSUMED -> "contrast";
                };
        return VaadinViewUtils.badge(VaadinViewUtils.enumLabel(status), theme);
    }

    private Component daysRemainingBadge(ItemInstance instance) {
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

    private long daysRemaining(ItemInstance instance) {
        int notificationDaysModifier =
                instance.getItemType() == null || instance.getItemType().getNotificationDaysModifier() == null
                        ? 0
                        : instance.getItemType().getNotificationDaysModifier();
        return ChronoUnit.DAYS.between(
                LocalDate.now(), instance.getBestBeforeDate().plusDays(notificationDaysModifier));
    }

    private void refreshGrid() {
        grid.setItems(itemInstanceService.getAllItemInstances());
    }
}
