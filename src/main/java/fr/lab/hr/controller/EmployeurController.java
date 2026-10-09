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

import fr.lab.hr.model.Employeur;
import fr.lab.hr.repository.EmployeurRepository;
import fr.lab.hr.repository.GradeRepository;

@Controller
@RequestMapping("/employeurs")
public class EmployeurController {

    private final EmployeurRepository repository;
    private final GradeRepository gradeRepository;

    public EmployeurController(EmployeurRepository repository, GradeRepository gradeRepository) {
        this.repository = repository;
        this.gradeRepository = gradeRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "employeur/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("employeur", new Employeur());
        model.addAttribute("gradeList", gradeRepository.findAll());
        return "employeur/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("employeur", repository.findById(id).orElseThrow());
        model.addAttribute("gradeList", gradeRepository.findAll());
        return "employeur/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("employeur", repository.findById(id).orElseThrow());
        return "employeur/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("employeur") Employeur employeur, BindingResult result,
            @RequestParam(required = false) List<Long> gradesIds, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("gradeList", gradeRepository.findAll());
            return "employeur/form";
        }
        employeur.setGrades(gradesIds != null ? gradeRepository.findAllById(gradesIds) : Collections.emptyList());
        repository.save(employeur);
        return "redirect:/employeurs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/employeurs";
    }
}
