package fr.lab.hr.controller;

import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.lab.hr.model.Structure;
import fr.lab.hr.repository.BureauRepository;
import fr.lab.hr.repository.StructureRepository;

@Controller
@RequestMapping("/structures")
public class StructureController {

    private final StructureRepository repository;
    private final BureauRepository bureauRepository;

    public StructureController(StructureRepository repository, BureauRepository bureauRepository) {
        this.repository = repository;
        this.bureauRepository = bureauRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "structure/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("structure", new Structure());
        model.addAttribute("structureList", repository.findAll());
        model.addAttribute("bureauList", bureauRepository.findAll());
        return "structure/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("structure", repository.findById(id).orElseThrow());
        model.addAttribute("structureList", repository.findAll());
        model.addAttribute("bureauList", bureauRepository.findAll());
        return "structure/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("structure", repository.findById(id).orElseThrow());
        return "structure/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("structure") Structure structure, BindingResult result,
            @RequestParam(required = false) Long structureParenteId,
            @RequestParam(required = false) List<Long> bureauxIds, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("structureList", repository.findAll());
            model.addAttribute("bureauList", bureauRepository.findAll());
            return "structure/form";
        }
        structure.setStructureParente(
                structureParenteId != null ? repository.findById(structureParenteId).orElse(null) : null);
        structure.setBureaux(bureauxIds != null ? bureauRepository.findAllById(bureauxIds) : Collections.emptyList());
        repository.save(structure);
        return "redirect:/structures";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/structures";
    }
}
