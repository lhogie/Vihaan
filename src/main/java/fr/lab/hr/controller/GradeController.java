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

import fr.lab.hr.model.Grade;
import fr.lab.hr.repository.GradeRepository;

@Controller
@RequestMapping("/grades")
public class GradeController {

    private final GradeRepository repository;

    public GradeController(GradeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "grade/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("grade", new Grade());
        return "grade/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("grade", repository.findById(id).orElseThrow());
        return "grade/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("grade", repository.findById(id).orElseThrow());
        return "grade/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("grade") Grade grade, BindingResult result) {
        if (result.hasErrors()) {
            return "grade/form";
        }
        repository.save(grade);
        return "redirect:/grades";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/grades";
    }
}
