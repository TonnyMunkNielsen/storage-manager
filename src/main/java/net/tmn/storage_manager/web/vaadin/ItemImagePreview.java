package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.service.UploadedImage;

final class ItemImagePreview extends Div {

    ItemImagePreview() {
        getStyle()
                .set("min-height", "180px")
                .set("border", "1px dashed var(--lumo-contrast-30pct)")
                .set("border-radius", "8px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "var(--lumo-space-m)");
    }

    void showUploadedOrItemType(UploadedImage uploadedImage, ItemType itemType, String emptyText) {
        removeAll();

        if (uploadedImage != null && !uploadedImage.isEmpty()) {
            add(previewImage(new Image(uploadedImage.bytes(), uploadedImage.filename(), uploadedImage.contentType())));
            return;
        }

        showItemType(itemType, emptyText);
    }

    void showItemType(ItemType itemType, String emptyText) {
        removeAll();

        if (VaadinViewUtils.hasImage(itemType)) {
            add(previewImage(new Image(VaadinViewUtils.imageUrl(itemType), itemType.getName())));
            return;
        }

        add(VaadinViewUtils.emptyText(emptyText));
    }

    private Image previewImage(Image image) {
        image.setMaxWidth("100%");
        image.setMaxHeight("220px");
        image.getStyle().set("object-fit", "contain").set("border-radius", "6px");
        return image;
    }
}
