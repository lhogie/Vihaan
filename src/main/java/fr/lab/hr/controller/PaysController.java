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

import fr.lab.hr.model.Pays;
import fr.lab.hr.repository.PaysRepository;

@Controller
@RequestMapping("/pays")
public class PaysController {

    private final PaysRepository repository;

    public PaysController(PaysRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "pays/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pays", new Pays());
        return "pays/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pays", repository.findById(id).orElseThrow());
        return "pays/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("pays", repository.findById(id).orElseThrow());
        return "pays/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("pays") Pays pays, BindingResult result) {
        if (result.hasErrors()) {
            return "pays/form";
        }
        repository.save(pays);
        return "redirect:/pays";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/pays";
    }
}
