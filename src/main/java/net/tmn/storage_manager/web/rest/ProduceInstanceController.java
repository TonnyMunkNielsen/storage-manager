package net.tmn.storage_manager.web.rest;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.tmn.storage_manager.database.jpa.ProduceInstance;
import net.tmn.storage_manager.database.jpa.type.ProduceInstanceStatus;
import net.tmn.storage_manager.service.ProduceInstanceService;
import net.tmn.storage_manager.service.ProduceTypeService;
import net.tmn.storage_manager.service.StorageBoxService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/produce-instances")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProduceInstanceController {

    ProduceInstanceService produceInstanceService;
    ProduceTypeService produceTypeService;
    StorageBoxService storageBoxService;

    @GetMapping
    public String listProduceInstances(Model model) {
        List<ProduceInstance> produceInstances = produceInstanceService.getAllProduceInstances();
        model.addAttribute("produceInstances", produceInstances);
        return "produce-instances/list";
    }

    @GetMapping("/new")
    public String newProduceInstanceForm(Model model) {
        ProduceInstance produceInstance = new ProduceInstance();

        // Default values in the front:
        produceInstance.setBestBeforeDate(LocalDate.now());
        produceInstance.setStatus(ProduceInstanceStatus.ACTIVE);

        model.addAttribute("produceInstance", produceInstance);
        model.addAttribute("produceTypes", produceTypeService.getAllProduceTypes());
        model.addAttribute("storageBoxes", storageBoxService.getAllStorageBoxes());

        return "produce-instances/form";
    }

    @PostMapping
    public String createProduceInstance(
            @Valid @ModelAttribute ProduceInstance produceInstance,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("produceTypes", produceTypeService.getAllProduceTypes());
            model.addAttribute("storageBoxes", storageBoxService.getAllStorageBoxes());
            return "produce-instances/form";
        }

        try {
            produceInstanceService.createProduceInstance(produceInstance);
            redirectAttributes.addFlashAttribute("success", "Produce instance created successfully!");
            return "redirect:/produce-instances";
        } catch (Exception e) {
            log.error("Error creating produce instance", e);
            redirectAttributes.addFlashAttribute("error", "Error creating produce instance: " + e.getMessage());
            return "redirect:/produce-instances/new";
        }
    }

    @GetMapping("/{id}")
    public String viewProduceInstance(@PathVariable UUID id, Model model) {
        ProduceInstance produceInstance = produceInstanceService
                .getProduceInstanceById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce instance not found"));

        model.addAttribute("produceInstance", produceInstance);
        return "produce-instances/view";
    }

    @GetMapping("/{id}/edit")
    public String editProduceInstanceForm(@PathVariable UUID id, Model model) {
        ProduceInstance produceInstance = produceInstanceService
                .getProduceInstanceById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produce instance not found"));

        model.addAttribute("produceInstance", produceInstance);
        model.addAttribute("produceTypes", produceTypeService.getAllProduceTypes());
        model.addAttribute("storageBoxes", storageBoxService.getAllStorageBoxes());
        return "produce-instances/form";
    }

    @PostMapping("/{id}")
    public String updateProduceInstance(
            @PathVariable UUID id,
            @Valid @ModelAttribute ProduceInstance produceInstance,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("produceTypes", produceTypeService.getAllProduceTypes());
            return "produce-instances/form";
        }

        try {
            produceInstanceService.updateProduceInstance(id, produceInstance);
            redirectAttributes.addFlashAttribute("success", "Produce instance updated successfully!");
            return "redirect:/produce-instances";
        } catch (Exception e) {
            log.error("Error updating produce instance", e);
            redirectAttributes.addFlashAttribute("error", "Error updating produce instance: " + e.getMessage());
            return "redirect:/produce-instances/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/replace")
    public String replaceProduceInstance(
            @PathVariable UUID id,
            @Valid @ModelAttribute ProduceInstance newInstance,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid produce instance data");
            return "redirect:/produce-instances/" + id;
        }

        try {
            produceInstanceService.replaceProduceInstance(id, newInstance);
            redirectAttributes.addFlashAttribute("success", "Produce instance replaced successfully!");
            return "redirect:/produce-instances";
        } catch (Exception e) {
            log.error("Error replacing produce instance", e);
            redirectAttributes.addFlashAttribute("error", "Error replacing produce instance: " + e.getMessage());
            return "redirect:/produce-instances/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteProduceInstance(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            produceInstanceService.deleteProduceInstance(id);
            redirectAttributes.addFlashAttribute("success", "Produce instance deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting produce instance", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting produce instance: " + e.getMessage());
        }
        return "redirect:/produce-instances";
    }
}
