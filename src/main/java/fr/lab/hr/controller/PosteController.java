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

import fr.lab.hr.model.Poste;
import fr.lab.hr.repository.EmployeurRepository;
import fr.lab.hr.repository.GradeRepository;
import fr.lab.hr.repository.PersonneRepository;
import fr.lab.hr.repository.PosteRepository;

@Controller
@RequestMapping("/postes")
public class PosteController {

    private final PosteRepository repository;
    private final EmployeurRepository employeurRepository;
    private final GradeRepository gradeRepository;
    private final PersonneRepository personneRepository;

    public PosteController(PosteRepository repository, EmployeurRepository employeurRepository,
            GradeRepository gradeRepository, PersonneRepository personneRepository) {
        this.repository = repository;
        this.employeurRepository = employeurRepository;
        this.gradeRepository = gradeRepository;
        this.personneRepository = personneRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "poste/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("poste", new Poste());
        addReferenceLists(model);
        return "poste/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("poste", repository.findById(id).orElseThrow());
        addReferenceLists(model);
        return "poste/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("poste", repository.findById(id).orElseThrow());
        return "poste/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("poste") Poste poste, BindingResult result,
            @RequestParam(required = false) Long employeurId, @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long personneId, Model model) {
        if (result.hasErrors()) {
            addReferenceLists(model);
            return "poste/form";
        }
        poste.setEmployeur(employeurId != null ? employeurRepository.findById(employeurId).orElse(null) : null);
        poste.setGrade(gradeId != null ? gradeRepository.findById(gradeId).orElse(null) : null);
        poste.setPersonne(personneId != null ? personneRepository.findById(personneId).orElse(null) : null);
        repository.save(poste);
        return "redirect:/postes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/postes";
    }

    private void addReferenceLists(Model model) {
        model.addAttribute("employeurList", employeurRepository.findAll());
        model.addAttribute("gradeList", gradeRepository.findAll());
        model.addAttribute("personneList", personneRepository.findAll());
    }
}
