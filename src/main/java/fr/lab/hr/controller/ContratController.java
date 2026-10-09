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

import fr.lab.hr.model.Contrat;
import fr.lab.hr.repository.ContratRepository;
import fr.lab.hr.repository.PersonneRepository;

@Controller
@RequestMapping("/contrats")
public class ContratController {

    private final ContratRepository repository;
    private final PersonneRepository personneRepository;

    public ContratController(ContratRepository repository, PersonneRepository personneRepository) {
        this.repository = repository;
        this.personneRepository = personneRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "contrat/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("contrat", new Contrat());
        model.addAttribute("personneList", personneRepository.findAll());
        return "contrat/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("contrat", repository.findById(id).orElseThrow());
        model.addAttribute("personneList", personneRepository.findAll());
        return "contrat/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("contrat", repository.findById(id).orElseThrow());
        return "contrat/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("contrat") Contrat contrat, BindingResult result,
            @RequestParam(required = false) Long porteurId, @RequestParam(required = false) List<Long> sousPorteursIds,
            @RequestParam(required = false) List<Long> coordinateursIds,
            @RequestParam(required = false) List<Long> partenairesIds,
            @RequestParam(required = false) List<Long> autresPersonnesIds, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("personneList", personneRepository.findAll());
            return "contrat/form";
        }
        contrat.setPorteur(porteurId != null ? personneRepository.findById(porteurId).orElse(null) : null);
        contrat.setSousPorteurs(idsToPersonnes(sousPorteursIds));
        contrat.setCoordinateurs(idsToPersonnes(coordinateursIds));
        contrat.setPartenaires(idsToPersonnes(partenairesIds));
        contrat.setAutresPersonnes(idsToPersonnes(autresPersonnesIds));
        repository.save(contrat);
        return "redirect:/contrats";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/contrats";
    }

    private List<fr.lab.hr.model.Personne> idsToPersonnes(List<Long> ids) {
        return ids != null ? personneRepository.findAllById(ids) : Collections.emptyList();
    }
}
