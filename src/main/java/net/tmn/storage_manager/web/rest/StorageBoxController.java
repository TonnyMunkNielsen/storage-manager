package net.tmn.storage_manager.web.rest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.tmn.storage_manager.database.jpa.StorageBox;
import net.tmn.storage_manager.service.StorageBoxService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/storage-boxes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StorageBoxController {

    StorageBoxService storageBoxService;

    @GetMapping
    public String listStorageBoxes(Model model) {
        List<StorageBox> storageBoxes = storageBoxService.getAllStorageBoxes();
        model.addAttribute("storageBoxes", storageBoxes);
        return "storage-boxes/list";
    }

    @GetMapping("/new")
    public String newStorageBoxForm(Model model) {
        model.addAttribute("storageBox", new StorageBox());
        return "storage-boxes/form";
    }

    @PostMapping
    public String createStorageBox(
            @Valid @ModelAttribute StorageBox storageBox, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "storage-boxes/form";
        }

        try {
            storageBoxService.createStorageBox(storageBox);
            redirectAttributes.addFlashAttribute("success", "Storage box created successfully!");
            return "redirect:/storage-boxes";
        } catch (Exception e) {
            log.error("Error creating storage box", e);
            redirectAttributes.addFlashAttribute("error", "Error creating storage box: " + e.getMessage());
            return "redirect:/storage-boxes/new";
        }
    }

    @GetMapping("/{id}")
    public String editStorageBoxForm(@PathVariable UUID id, Model model) {
        StorageBox storageBox = storageBoxService
                .getStorageBoxById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage box not found"));
        model.addAttribute("storageBox", storageBox);
        return "storage-boxes/form";
    }

    @PostMapping("/{id}")
    public String updateStorageBox(
            @PathVariable UUID id,
            @Valid @ModelAttribute StorageBox storageBox,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "storage-boxes/form";
        }

        try {
            storageBoxService.updateStorageBox(id, storageBox);
            redirectAttributes.addFlashAttribute("success", "Storage box updated successfully!");
            return "redirect:/storage-boxes";
        } catch (Exception e) {
            log.error("Error updating storage box", e);
            redirectAttributes.addFlashAttribute("error", "Error updating storage box: " + e.getMessage());
            return "redirect:/storage-boxes/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteStorageBox(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            storageBoxService.deleteStorageBox(id);
            redirectAttributes.addFlashAttribute("success", "Storage box deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting storage box", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting storage box: " + e.getMessage());
        }
        return "redirect:/storage-boxes";
    }
}
