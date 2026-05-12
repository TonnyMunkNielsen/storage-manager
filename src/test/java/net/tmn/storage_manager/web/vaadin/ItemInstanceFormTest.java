package net.tmn.storage_manager.web.vaadin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.vaadin.flow.data.binder.ValidationException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import net.tmn.storage_manager.database.jpa.ItemInstance;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.database.jpa.type.ItemInstanceStatus;
import net.tmn.storage_manager.database.jpa.type.StorageBoxStatus;
import org.junit.jupiter.api.Test;

class ItemInstanceFormTest {

    @Test
    void beanValidationRejectsMissingRequiredFields() {
        ItemInstanceForm form = new ItemInstanceForm();
        form.setSelectableItems(List.of(itemType()), List.of(storageBox()));
        ItemInstance instance = new ItemInstance();
        instance.setStatus(ItemInstanceStatus.ACTIVE);

        form.readBean(instance, true);

        assertFalse(form.isValid());
    }

    @Test
    void writeBeanStoresSelectionsAndTrimsTitle() throws ValidationException {
        ItemType itemType = itemType();
        StorageBox storageBox = storageBox();
        LocalDate bestBeforeDate = LocalDate.now().plusDays(30);
        ItemInstanceForm form = new ItemInstanceForm();
        form.setSelectableItems(List.of(itemType), List.of(storageBox));
        form.itemType.setValue(itemType);
        form.title.setValue("  Nozzle  ");
        form.bestBeforeDate.setValue(bestBeforeDate);
        form.storageBox.setValue(storageBox);
        form.status.setValue(ItemInstanceStatus.ACTIVE);

        ItemInstance instance = new ItemInstance();
        form.writeBean(instance);

        assertEquals(itemType, instance.getItemType());
        assertEquals("Nozzle", instance.getTitle());
        assertEquals(bestBeforeDate, instance.getBestBeforeDate());
        assertEquals(storageBox, instance.getStorageBox());
        assertEquals(ItemInstanceStatus.ACTIVE, instance.getStatus());
    }

    private ItemType itemType() {
        ItemType itemType = new ItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setName("Filament");
        itemType.setNotificationDaysModifier(0);
        return itemType;
    }

    private StorageBox storageBox() {
        StorageBox storageBox = new StorageBox();
        storageBox.setId(UUID.randomUUID());
        storageBox.setBoxNumber(1);
        storageBox.setDessicantChangedDate(LocalDate.now());
        storageBox.setStatus(StorageBoxStatus.ACTIVE);
        return storageBox;
    }
}
