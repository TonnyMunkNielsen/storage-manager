package net.tmn.storage_manager.web.rest;

import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.tmn.storage_manager.database.jpa.ProduceType;
import net.tmn.storage_manager.service.ProduceTypeService;
import net.tmn.storage_manager.service.ProduceTypeTransferData;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Controller
@RequestMapping("/produce-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProduceTypeController {

    ProduceTypeService produceTypeService;
    JsonMapper jsonMapper;

    @GetMapping
    public String listProduceTypes(Model model) {
        List<ProduceType> produceTypes = produceTypeService.getAllProduceTypes();
        model.addAttribute("produceTypes", produceTypes);
        return "produce-types/list";
    }

    @GetMapping("/export")
    public ResponseEntity<ProduceTypeTransferData> exportProduceTypes() {
        String filename =
                "produce-types-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(produceTypeService.exportProduceTypes());
    }

    @PostMapping("/import")
    public String importProduceTypes(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Select an exported produce type file to import.");
            return "redirect:/produce-types";
        }

        try {
            ProduceTypeTransferData transferData =
                    jsonMapper.readValue(file.getInputStream(), ProduceTypeTransferData.class);
            int importedCount = produceTypeService.importProduceTypes(transferData);
            redirectAttributes.addFlashAttribute(
                    "success", "Imported " + importedCount + " produce type" + (importedCount == 1 ? "" : "s") + ".");
        } catch (IOException | JacksonException e) {
            log.error("Error importing produce types", e);
            redirectAttributes.addFlashAttribute("error", "Error importing produce types: " + e.getMessage());
        }

        return "redirect:/produce-types";
    }

    @GetMapping("/new")
    public String newProduceTypeForm(Model model) {
        ProduceType produceType = new ProduceType();

        // Default values
        produceType.setNotificationDaysModifier(0);
        produceType.setPrice(BigDecimal.ZERO);

        model.addAttribute("produceType", produceType);
        return "produce-types/form";
    }

    @PostMapping
    public String createProduceType(
            @Valid @ModelAttribute ProduceType produceType,
            BindingResult result,
            @RequestParam(value = "image", required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "produce-types/form";
        }

        try {
            produceTypeService.createProduceType(produceType, image);
            redirectAttributes.addFlashAttribute("success", "Produce type created successfully!");
            return "redirect:/produce-types";
        } catch (Exception e) {
            log.error("Error creating produce type", e);
            redirectAttributes.addFlashAttribute("error", "Error creating produce type: " + e.getMessage());
            return "redirect:/produce-types/new";
        }
    }

    @GetMapping("/{id}")
    public String viewProduceType(@PathVariable UUID id, Model model) {
        ProduceType produceType = produceTypeService
                .getProduceTypeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce type not found"));

        model.addAttribute("produceType", produceType);
        return "produce-types/view";
    }

    @GetMapping("/{id}/edit")
    public String editProduceTypeForm(@PathVariable UUID id, Model model) {
        ProduceType produceType = produceTypeService
                .getProduceTypeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce type not found"));

        model.addAttribute("produceType", produceType);
        return "produce-types/form";
    }

    @PostMapping("/{id}")
    public String updateProduceType(
            @PathVariable UUID id,
            @Valid @ModelAttribute ProduceType produceType,
            BindingResult result,
            @RequestParam(value = "image", required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "produce-types/form";
        }

        try {
            produceTypeService.updateProduceType(id, produceType, image);
            redirectAttributes.addFlashAttribute("success", "Produce type updated successfully!");
            return "redirect:/produce-types";
        } catch (Exception e) {
            log.error("Error updating produce type", e);
            redirectAttributes.addFlashAttribute("error", "Error updating produce type: " + e.getMessage());
            return "redirect:/produce-types/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteProduceType(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            produceTypeService.deleteProduceType(id);
            redirectAttributes.addFlashAttribute("success", "Produce type deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting produce type", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting produce type: " + e.getMessage());
        }
        return "redirect:/produce-types";
    }
}
