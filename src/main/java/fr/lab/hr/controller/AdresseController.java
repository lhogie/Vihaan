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

import fr.lab.hr.model.Adresse;
import fr.lab.hr.repository.AdresseRepository;
import fr.lab.hr.repository.PaysRepository;

@Controller
@RequestMapping("/adresses")
public class AdresseController {

    private final AdresseRepository repository;
    private final PaysRepository paysRepository;

    public AdresseController(AdresseRepository repository, PaysRepository paysRepository) {
        this.repository = repository;
        this.paysRepository = paysRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "adresse/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("adresse", new Adresse());
        model.addAttribute("paysList", paysRepository.findAll());
        return "adresse/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("adresse", repository.findById(id).orElseThrow());
        model.addAttribute("paysList", paysRepository.findAll());
        return "adresse/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("adresse", repository.findById(id).orElseThrow());
        return "adresse/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("adresse") Adresse adresse, BindingResult result,
            @RequestParam(required = false) Long paysId, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("paysList", paysRepository.findAll());
            return "adresse/form";
        }
        adresse.setPays(paysId != null ? paysRepository.findById(paysId).orElse(null) : null);
        repository.save(adresse);
        return "redirect:/adresses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/adresses";
    }
}
