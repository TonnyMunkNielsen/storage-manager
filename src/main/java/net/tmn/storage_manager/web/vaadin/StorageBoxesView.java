package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.jpa.type.StorageBoxStatus;
import net.tmn.storage_manager.service.StorageBoxService;

@Route(value = "storage-boxes", layout = MainLayout.class)
@PageTitle("Storage Boxes")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageBoxesView extends VerticalLayout {

    final StorageBoxService storageBoxService;
    final Grid<StorageBox> grid = new Grid<>(StorageBox.class, false);
    final Binder<StorageBox> binder = new Binder<>(StorageBox.class);
    final Dialog dialog = new Dialog();
    final IntegerField boxNumber = new IntegerField("Box number");
    final DatePicker dessicantChangedDate = new DatePicker("Dessicant changed date");
    final ComboBox<StorageBoxStatus> status = new ComboBox<>("Status");

    StorageBox editedBox;

    public StorageBoxesView(StorageBoxService storageBoxService) {
        this.storageBoxService = storageBoxService;

        setSizeFull();
        setPadding(true);
        configureGrid();
        configureBinder();
        add(createHeader(), grid);
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H2 header = new H2("Storage Boxes");
        header.getStyle().set("margin", "0");

        Button create = VaadinViewUtils.primaryButton("Add Storage Box", VaadinIcon.PLUS);
        create.addClickListener(event -> openEditor(newStorageBox()));

        HorizontalLayout layout = new HorizontalLayout(header, create);
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return layout;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setEmptyStateText("No storage boxes found.");
        grid.addColumn(StorageBox::getBoxNumber).setHeader("Box Number").setAutoWidth(true);
        grid.addColumn(box -> VaadinViewUtils.formatDate(box.getDessicantChangedDate()))
                .setHeader("Dessicant Changed")
                .setAutoWidth(true);
        grid.addComponentColumn(box -> VaadinViewUtils.badge(VaadinViewUtils.enumLabel(box.getStatus()), "contrast"))
                .setHeader("Status")
                .setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("Actions").setAutoWidth(true);
    }

    private HorizontalLayout actions(StorageBox box) {
        Button edit = VaadinViewUtils.iconButton(VaadinIcon.EDIT, "Edit");
        edit.addClickListener(event -> openEditor(box));

        Button delete = VaadinViewUtils.iconButton(VaadinIcon.TRASH, "Delete");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> confirmDelete(box));

        return new HorizontalLayout(edit, delete);
    }

    private void configureBinder() {
        boxNumber.setMin(1);
        status.setItems(StorageBoxStatus.values());
        status.setItemLabelGenerator(VaadinViewUtils::enumLabel);

        binder.forField(boxNumber)
                .asRequired("Box number is required")
                .withValidator(value -> value != null && value > 0, "Box number must be positive")
                .bind(StorageBox::getBoxNumber, StorageBox::setBoxNumber);
        binder.forField(dessicantChangedDate)
                .asRequired("Dessicant changed date is required")
                .bind(StorageBox::getDessicantChangedDate, StorageBox::setDessicantChangedDate);
        binder.forField(status).asRequired("Status is required").bind(StorageBox::getStatus, StorageBox::setStatus);
    }

    private StorageBox newStorageBox() {
        StorageBox storageBox = new StorageBox();
        storageBox.setDessicantChangedDate(LocalDate.now());
        storageBox.setStatus(StorageBoxStatus.ACTIVE);
        return storageBox;
    }

    private void openEditor(StorageBox storageBox) {
        editedBox = storageBox;
        boolean createMode = storageBox.getId() == null;

        dialog.removeAll();
        dialog.setHeaderTitle(createMode ? "Add Storage Box" : "Edit Storage Box");
        boxNumber.setReadOnly(!createMode);
        binder.readBean(editedBox);

        FormLayout form = new FormLayout(boxNumber, dessicantChangedDate, status);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("560px", 2));

        Button cancel = new Button("Cancel", event -> dialog.close());
        Button save = VaadinViewUtils.primaryButton("Save", VaadinIcon.CHECK);
        save.addClickListener(event -> save());

        HorizontalLayout buttons = new HorizontalLayout(cancel, save);
        buttons.setJustifyContentMode(JustifyContentMode.END);
        buttons.setWidthFull();

        dialog.add(new VerticalLayout(form, buttons));
        dialog.open();
    }

    private void save() {
        try {
            binder.writeBean(editedBox);
            if (editedBox.getId() == null) {
                storageBoxService.createStorageBox(editedBox);
                VaadinViewUtils.success("Storage box created.");
            } else {
                storageBoxService.updateStorageBox(editedBox.getId(), editedBox);
                VaadinViewUtils.success("Storage box updated.");
            }
            dialog.close();
            refreshGrid();
        } catch (ValidationException e) {
            VaadinViewUtils.error("Check the highlighted fields.");
        } catch (RuntimeException e) {
            VaadinViewUtils.error(e.getMessage());
        }
    }

    private void confirmDelete(StorageBox storageBox) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete storage box");
        confirm.setText("Delete storage box #" + storageBox.getBoxNumber() + "?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(event -> {
            try {
                storageBoxService.deleteStorageBox(storageBox.getId());
                VaadinViewUtils.success("Storage box deleted.");
                refreshGrid();
            } catch (RuntimeException e) {
                VaadinViewUtils.error(e.getMessage());
            }
        });
        confirm.open();
    }

    private void refreshGrid() {
        grid.setItems(storageBoxService.getAllStorageBoxes());
    }
}
