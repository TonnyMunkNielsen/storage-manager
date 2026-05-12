package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.streams.UploadHandler;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.service.UploadedImage;

final class ItemTypeForm extends FormLayout {

    static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    final BeanValidationBinder<ItemType> binder = new BeanValidationBinder<>(ItemType.class);
    final TextField name = new TextField("Name");
    final TextArea description = new TextArea("Description");
    final BigDecimalField price = new BigDecimalField("Price");
    final IntegerField notificationDaysModifier = new IntegerField("Notification days modifier");
    final ItemImagePreview imagePreview = new ItemImagePreview();

    ItemType itemType;
    UploadedImage uploadedImage;

    ItemTypeForm() {
        configureFields();
        configureBinder();
        add(name, description, price, notificationDaysModifier, createImageUpload(), imagePreview);
        setColspan(description, 2);
        setColspan(imagePreview, 2);
        setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("680px", 2));
    }

    void readBean(ItemType itemType) {
        this.itemType = itemType;
        uploadedImage = null;
        binder.readBean(itemType);
        updateImagePreview();
    }

    void writeBean(ItemType itemType) throws ValidationException {
        binder.writeBean(itemType);
        if (itemType.getName() != null) {
            itemType.setName(itemType.getName().trim());
        }
    }

    UploadedImage uploadedImage() {
        return uploadedImage;
    }

    boolean isValid() {
        return binder.validate().isOk();
    }

    private void configureFields() {
        name.setRequiredIndicatorVisible(true);
        description.setMinHeight("110px");
        price.setPrefixComponent(new Span("kr."));
        notificationDaysModifier.setMin(0);
        notificationDaysModifier.setRequiredIndicatorVisible(true);
    }

    private void configureBinder() {
        binder.forField(name).bind("name");
        binder.forField(description).bind("description");
        binder.forField(price).bind("price");
        binder.forField(notificationDaysModifier).bind("notificationDaysModifier");
    }

    private Upload createImageUpload() {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, bytes) -> {
            uploadedImage = new UploadedImage(metadata.fileName(), metadata.contentType(), bytes);
            updateImagePreview();
        }));
        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_IMAGE_SIZE);
        upload.setAcceptedFileTypes(
                "image/jpeg", "image/png", "image/gif", "image/webp", ".jpg", ".jpeg", ".png", ".gif", ".webp");
        upload.setDropAllowed(true);
        upload.setUploadButton(new Button("Upload Image", VaadinIcon.UPLOAD.create()));
        upload.addFileRejectedListener(event -> VaadinViewUtils.error(event.getErrorMessage()));
        upload.addFileRemovedListener(event -> {
            uploadedImage = null;
            updateImagePreview();
        });
        return upload;
    }

    private void updateImagePreview() {
        imagePreview.showUploadedOrItemType(uploadedImage, itemType, "No image selected.");
    }
}
