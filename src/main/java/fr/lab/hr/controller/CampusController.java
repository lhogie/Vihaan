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

import fr.lab.hr.model.Campus;
import fr.lab.hr.repository.CampusRepository;

@Controller
@RequestMapping("/campus")
public class CampusController {

    private final CampusRepository repository;

    public CampusController(CampusRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "campus/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("campus", new Campus());
        return "campus/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("campus", repository.findById(id).orElseThrow());
        return "campus/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("campus", repository.findById(id).orElseThrow());
        return "campus/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("campus") Campus campus, BindingResult result) {
        if (result.hasErrors()) {
            return "campus/form";
        }
        repository.save(campus);
        return "redirect:/campus";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/campus";
    }
}
