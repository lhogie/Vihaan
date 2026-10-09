package fr.lab.hr.controller;

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

import fr.lab.hr.model.Batiment;
import fr.lab.hr.repository.BatimentRepository;
import fr.lab.hr.repository.CampusRepository;

@Controller
@RequestMapping("/batiments")
public class BatimentController {

    private final BatimentRepository repository;
    private final CampusRepository campusRepository;

    public BatimentController(BatimentRepository repository, CampusRepository campusRepository) {
        this.repository = repository;
        this.campusRepository = campusRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "batiment/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("batiment", new Batiment());
        model.addAttribute("campusList", campusRepository.findAll());
        return "batiment/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("batiment", repository.findById(id).orElseThrow());
        model.addAttribute("campusList", campusRepository.findAll());
        return "batiment/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("batiment", repository.findById(id).orElseThrow());
        return "batiment/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("batiment") Batiment batiment, BindingResult result,
            @RequestParam(required = false) Long campusId, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("campusList", campusRepository.findAll());
            return "batiment/form";
        }
        batiment.setCampus(campusId != null ? campusRepository.findById(campusId).orElse(null) : null);
        repository.save(batiment);
        return "redirect:/batiments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/batiments";
    }
}
