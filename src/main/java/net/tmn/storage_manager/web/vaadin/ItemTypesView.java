package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.service.ItemTypeService;
import net.tmn.storage_manager.service.ItemTypeTransferData;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Route(value = "item-types", layout = MainLayout.class)
@PageTitle("Item Types")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemTypesView extends VerticalLayout {

    static final int MAX_IMPORT_SIZE = 100 * 1024 * 1024;
    static final DateTimeFormatter EXPORT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    final ItemTypeService itemTypeService;
    final JsonMapper jsonMapper;
    final Grid<ItemType> grid = new Grid<>(ItemType.class, false);
    final Dialog editorDialog = new Dialog();
    final ItemTypeForm itemTypeForm = new ItemTypeForm();

    ItemType editedItemType;

    public ItemTypesView(ItemTypeService itemTypeService, JsonMapper jsonMapper) {
        this.itemTypeService = itemTypeService;
        this.jsonMapper = jsonMapper;

        setSizeFull();
        setPadding(true);
        configureGrid();
        add(createHeader(), grid);
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H2 header = new H2("Item Types");
        header.getStyle().set("margin", "0");

        Button create = VaadinViewUtils.primaryButton("Add Item Type", VaadinIcon.PLUS);
        create.addClickListener(event -> openEditor(newItemType()));

        HorizontalLayout actions = new HorizontalLayout(create, createExportAnchor(), createImportUpload());
        actions.setAlignItems(Alignment.CENTER);
        actions.getStyle().set("flex-wrap", "wrap");

        HorizontalLayout layout = new HorizontalLayout(header, actions);
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        layout.getStyle().set("gap", "var(--lumo-space-m)").set("flex-wrap", "wrap");
        return layout;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setEmptyStateText("No item types found.");
        grid.addComponentColumn(this::thumbnailOrEmpty)
                .setHeader("Image")
                .setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(ItemType::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(ItemType::getDescription).setHeader("Description").setFlexGrow(1);
        grid.addColumn(type -> VaadinViewUtils.formatMoney(type.getPrice()))
                .setHeader("Price")
                .setAutoWidth(true);
        grid.addColumn(type -> safeNotificationDays(type) + " days")
                .setHeader("Notification Days")
                .setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("Actions").setAutoWidth(true);
    }

    private Component thumbnailOrEmpty(ItemType itemType) {
        if (!VaadinViewUtils.hasImage(itemType)) {
            return VaadinViewUtils.emptyText("No image");
        }

        Image image = VaadinViewUtils.thumbnail(itemType);
        image.addClickListener(event -> openImageDialog(itemType));
        return image;
    }

    private HorizontalLayout actions(ItemType itemType) {
        Button edit = VaadinViewUtils.iconButton(VaadinIcon.EDIT, "Edit");
        edit.addClickListener(event -> openEditor(itemType));

        Button delete = VaadinViewUtils.iconButton(VaadinIcon.TRASH, "Delete");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> confirmDelete(itemType));

        return new HorizontalLayout(edit, delete);
    }

    private ItemType newItemType() {
        ItemType itemType = new ItemType();
        itemType.setPrice(BigDecimal.ZERO);
        itemType.setNotificationDaysModifier(0);
        return itemType;
    }

    private void openEditor(ItemType itemType) {
        editedItemType = itemType;

        editorDialog.removeAll();
        editorDialog.setHeaderTitle(itemType.getId() == null ? "Add Item Type" : "Edit Item Type");
        itemTypeForm.readBean(editedItemType);

        Button cancel = new Button("Cancel", event -> editorDialog.close());
        Button save = VaadinViewUtils.primaryButton("Save", VaadinIcon.CHECK);
        save.addClickListener(event -> save());

        HorizontalLayout buttons = new HorizontalLayout(cancel, save);
        buttons.setJustifyContentMode(JustifyContentMode.END);
        buttons.setWidthFull();

        editorDialog.add(new VerticalLayout(itemTypeForm, buttons));
        editorDialog.open();
    }

    private void save() {
        try {
            itemTypeForm.writeBean(editedItemType);

            if (editedItemType.getId() == null) {
                itemTypeService.createItemType(editedItemType, itemTypeForm.uploadedImage());
                VaadinViewUtils.success("Item type created.");
            } else {
                itemTypeService.updateItemType(editedItemType.getId(), editedItemType, itemTypeForm.uploadedImage());
                VaadinViewUtils.success("Item type updated.");
            }
            editorDialog.close();
            refreshGrid();
        } catch (ValidationException e) {
            VaadinViewUtils.error("Check the highlighted fields.");
        } catch (RuntimeException e) {
            VaadinViewUtils.error(VaadinViewUtils.validationMessage(e));
        }
    }

    private Anchor createExportAnchor() {
        DownloadHandler downloadHandler = DownloadHandler.fromInputStream(event -> createExportResponse());
        Anchor anchor = new Anchor(downloadHandler, "");
        anchor.setDownload(true);
        Button export = new Button("Export Data", VaadinIcon.DOWNLOAD.create());
        anchor.add(export);
        return anchor;
    }

    private DownloadResponse createExportResponse() {
        byte[] bytes;
        try {
            bytes = jsonMapper.writeValueAsBytes(itemTypeService.exportItemTypes());
        } catch (JacksonException e) {
            bytes = "{\"error\":\"Failed to export item types\"}".getBytes(StandardCharsets.UTF_8);
        }
        return new DownloadResponse(
                new ByteArrayInputStream(bytes), exportFilename(), "application/json", bytes.length);
    }

    private String exportFilename() {
        return "item-types-" + LocalDateTime.now().format(EXPORT_FORMATTER) + ".json";
    }

    private Upload createImportUpload() {
        Upload upload = new Upload();
        upload.setUploadHandler(UploadHandler.inMemory((metadata, bytes) -> confirmImport(bytes, upload)));
        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_IMPORT_SIZE);
        upload.setAcceptedFileTypes("application/json", ".json");
        upload.setDropAllowed(false);
        upload.setUploadButton(new Button("Import Data", VaadinIcon.UPLOAD_ALT.create()));
        upload.addFileRejectedListener(event -> VaadinViewUtils.error(event.getErrorMessage()));
        return upload;
    }

    private void confirmImport(byte[] bytes, Upload upload) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Import item types");
        confirm.setText("Importing creates new item types and updates existing ones with matching names.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Import");
        confirm.setConfirmButtonTheme("primary");
        confirm.addConfirmListener(event -> {
            try {
                ItemTypeTransferData transferData =
                        jsonMapper.readValue(new ByteArrayInputStream(bytes), ItemTypeTransferData.class);
                int importedCount = itemTypeService.importItemTypes(transferData);
                VaadinViewUtils.success("Imported " + importedCount + " item type" + (importedCount == 1 ? "." : "s."));
                refreshGrid();
            } catch (JacksonException e) {
                VaadinViewUtils.error("Import failed: " + e.getMessage());
            } catch (RuntimeException e) {
                VaadinViewUtils.error("Import failed: " + VaadinViewUtils.validationMessage(e));
            } finally {
                upload.clearFileList();
            }
        });
        confirm.addCancelListener(event -> upload.clearFileList());
        confirm.open();
    }

    private void openImageDialog(ItemType itemType) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(itemType.getName());
        Image image = new Image(VaadinViewUtils.imageUrl(itemType), itemType.getName());
        image.setMaxWidth("82vw");
        image.setMaxHeight("72vh");
        image.getStyle().set("object-fit", "contain");
        Button close = new Button("Close", event -> dialog.close());
        dialog.add(new VerticalLayout(image, close));
        dialog.open();
    }

    private void confirmDelete(ItemType itemType) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete item type");
        confirm.setText("Delete " + itemType.getName() + "?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(event -> {
            try {
                itemTypeService.deleteItemType(itemType.getId());
                VaadinViewUtils.success("Item type deleted.");
                refreshGrid();
            } catch (RuntimeException e) {
                VaadinViewUtils.error(VaadinViewUtils.validationMessage(e));
            }
        });
        confirm.open();
    }

    private int safeNotificationDays(ItemType type) {
        Integer notificationDaysModifierVar = type.getNotificationDaysModifier();
        return notificationDaysModifierVar == null ? 0 : notificationDaysModifierVar;
    }

    private void refreshGrid() {
        grid.setItems(itemTypeService.getAllItemTypes());
    }
}
