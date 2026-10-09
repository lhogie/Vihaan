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

import fr.lab.hr.model.Bureau;
import fr.lab.hr.repository.BatimentRepository;
import fr.lab.hr.repository.BureauRepository;

@Controller
@RequestMapping("/bureaux")
public class BureauController {

    private final BureauRepository repository;
    private final BatimentRepository batimentRepository;

    public BureauController(BureauRepository repository, BatimentRepository batimentRepository) {
        this.repository = repository;
        this.batimentRepository = batimentRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "bureau/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("bureau", new Bureau());
        model.addAttribute("batimentList", batimentRepository.findAll());
        return "bureau/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("bureau", repository.findById(id).orElseThrow());
        model.addAttribute("batimentList", batimentRepository.findAll());
        return "bureau/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("bureau", repository.findById(id).orElseThrow());
        return "bureau/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("bureau") Bureau bureau, BindingResult result,
            @RequestParam(required = false) Long batimentId, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("batimentList", batimentRepository.findAll());
            return "bureau/form";
        }
        bureau.setBatiment(batimentId != null ? batimentRepository.findById(batimentId).orElse(null) : null);
        repository.save(bureau);
        return "redirect:/bureaux";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/bureaux";
    }
}
