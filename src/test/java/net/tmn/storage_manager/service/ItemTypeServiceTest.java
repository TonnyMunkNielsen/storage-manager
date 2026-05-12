package net.tmn.storage_manager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.List;
import net.tmn.storage_manager.database.jpa.ItemType;
import net.tmn.storage_manager.database.repository.ItemTypeRepository;
import net.tmn.storage_manager.service.validation.DomainValidator;
import net.tmn.storage_manager.service.validation.ImageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemTypeServiceTest {

    @Mock
    ItemTypeRepository itemTypeRepository;

    ItemTypeService itemTypeService;

    @BeforeEach
    void setUp() {
        DomainValidator domainValidator =
                new DomainValidator(Validation.buildDefaultValidatorFactory().getValidator());
        itemTypeService = new ItemTypeService(itemTypeRepository, new ImageValidator(), domainValidator);
    }

    @Test
    void createItemTypeTrimsNameBeforeCheckingUniqueness() {
        ItemType itemType = new ItemType();
        itemType.setName("  Filament  ");
        itemType.setPrice(BigDecimal.ZERO);
        itemType.setNotificationDaysModifier(0);
        when(itemTypeRepository.existsByName("Filament")).thenReturn(false);
        when(itemTypeRepository.save(itemType)).thenReturn(itemType);

        ItemType saved = itemTypeService.createItemType(itemType, (UploadedImage) null);

        assertEquals("Filament", saved.getName());
        verify(itemTypeRepository).existsByName("Filament");
        verify(itemTypeRepository).save(itemType);
    }

    @Test
    void importItemTypesRejectsInvalidBeanConstraintsBeforeSaving() {
        ItemTypeTransferData transferData = new ItemTypeTransferData(List.of(new ItemTypeTransferData.ItemTypeRecord(
                "Invalid", null, BigDecimal.valueOf(-1), -1, null, null, null)));

        assertThrows(ConstraintViolationException.class, () -> itemTypeService.importItemTypes(transferData));
        verifyNoInteractions(itemTypeRepository);
    }

    @Test
    void importItemTypesRejectsInvalidImageContentTypeBeforeSaving() {
        ItemTypeTransferData transferData = new ItemTypeTransferData(List.of(new ItemTypeTransferData.ItemTypeRecord(
                "Invalid image", null, BigDecimal.ZERO, 0, "image.txt", "text/plain", new byte[] {1})));

        assertThrows(IllegalArgumentException.class, () -> itemTypeService.importItemTypes(transferData));
        verifyNoInteractions(itemTypeRepository);
    }
}
