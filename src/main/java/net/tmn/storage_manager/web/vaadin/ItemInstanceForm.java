package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.tmn.storage_manager.database.jpa.ItemInstance;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.jpa.type.ItemInstanceStatus;

final class ItemInstanceForm extends FormLayout {

    final BeanValidationBinder<ItemInstance> binder = new BeanValidationBinder<>(ItemInstance.class);
    final ComboBox<ItemType> itemType = new ComboBox<>("Item type");
    final TextField title = new TextField("Title");
    final DatePicker bestBeforeDate = new DatePicker("Best-before date");
    final ComboBox<StorageBox> storageBox = new ComboBox<>("Storage box");
    final ComboBox<ItemInstanceStatus> status = new ComboBox<>("Status");
    final ItemImagePreview itemTypePreview = new ItemImagePreview();

    List<ItemType> itemTypes = List.of();
    List<StorageBox> storageBoxes = List.of();
    ItemInstance itemInstance;

    ItemInstanceForm() {
        configureFields();
        configureBinder();
        add(itemType, title, bestBeforeDate, storageBox, status, itemTypePreview);
        setColspan(itemTypePreview, 2);
        setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("680px", 2));
    }

    void setSelectableItems(List<ItemType> itemTypes, List<StorageBox> storageBoxes) {
        this.itemTypes = List.copyOf(itemTypes);
        this.storageBoxes = List.copyOf(storageBoxes);
        itemType.setItems(this.itemTypes);
        storageBox.setItems(this.storageBoxes);
    }

    void readBean(ItemInstance itemInstance, boolean createMode) {
        this.itemInstance = itemInstance;
        itemType.setReadOnly(!createMode);
        status.setVisible(!createMode);
        binder.readBean(itemInstance);
        selectMatchingValues();
        updateItemTypePreview(itemType.getValue());
    }

    void writeBean(ItemInstance itemInstance) throws ValidationException {
        binder.writeBean(itemInstance);
        if (itemInstance.getTitle() != null) {
            itemInstance.setTitle(itemInstance.getTitle().trim());
        }
    }

    boolean isValid() {
        return binder.validate().isOk();
    }

    private void configureFields() {
        itemType.setItemLabelGenerator(ItemType::getName);
        itemType.setRequiredIndicatorVisible(true);
        itemType.addValueChangeListener(event -> updateItemTypePreview(event.getValue()));

        title.setRequiredIndicatorVisible(true);
        storageBox.setItemLabelGenerator(box -> "Box #" + box.getBoxNumber());
        storageBox.setRequiredIndicatorVisible(true);
        status.setItems(ItemInstanceStatus.values());
        status.setItemLabelGenerator(VaadinViewUtils::enumLabel);
        status.setRequiredIndicatorVisible(true);
    }

    private void configureBinder() {
        binder.forField(itemType).bind("itemType");
        binder.forField(title).bind("title");
        binder.forField(bestBeforeDate).bind("bestBeforeDate");
        binder.forField(storageBox).bind("storageBox");
        binder.forField(status).bind("status");
    }

    private void selectMatchingValues() {
        if (itemInstance.getItemType() != null) {
            findMatchingItemType(itemInstance.getItemType()).ifPresent(itemType::setValue);
        }

        if (itemInstance.getStorageBox() != null) {
            findMatchingStorageBox(itemInstance.getStorageBox()).ifPresent(storageBox::setValue);
        }
    }

    private Optional<ItemType> findMatchingItemType(ItemType selected) {
        return itemTypes.stream()
                .filter(type -> Objects.equals(type.getId(), selected.getId()))
                .findFirst();
    }

    private Optional<StorageBox> findMatchingStorageBox(StorageBox selected) {
        return storageBoxes.stream()
                .filter(box -> Objects.equals(box.getId(), selected.getId()))
                .findFirst();
    }

    private void updateItemTypePreview(ItemType selectedItemType) {
        itemTypePreview.showItemType(selectedItemType, "No image available.");
    }
}
