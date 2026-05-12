package net.tmn.storage_manager.web.vaadin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.vaadin.flow.data.binder.ValidationException;
import java.math.BigDecimal;
import net.tmn.storage_manager.database.jpa.ItemType;
import org.junit.jupiter.api.Test;

class ItemTypeFormTest {

    @Test
    void beanValidationRejectsMissingName() {
        ItemTypeForm form = new ItemTypeForm();
        ItemType itemType = new ItemType();
        itemType.setPrice(BigDecimal.ZERO);
        itemType.setNotificationDaysModifier(0);

        form.readBean(itemType);

        assertFalse(form.isValid());
    }

    @Test
    void beanValidationRejectsNegativeNumbers() {
        ItemTypeForm form = new ItemTypeForm();
        form.name.setValue("Filament");
        form.price.setValue(BigDecimal.valueOf(-1));
        form.notificationDaysModifier.setValue(-1);

        assertFalse(form.isValid());
    }

    @Test
    void writeBeanTrimsName() throws ValidationException {
        ItemTypeForm form = new ItemTypeForm();
        ItemType itemType = new ItemType();
        form.name.setValue("  Filament  ");
        form.price.setValue(BigDecimal.ZERO);
        form.notificationDaysModifier.setValue(0);

        form.writeBean(itemType);

        assertEquals("Filament", itemType.getName());
    }
}
