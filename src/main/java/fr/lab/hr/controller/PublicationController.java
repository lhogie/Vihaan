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
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.lab.hr.hal.ImportHal;
import fr.lab.hr.model.Publication;
import fr.lab.hr.repository.PublicationRepository;

@Controller
@RequestMapping("/publications")
public class PublicationController {

    private final PublicationRepository repository;
    private final ImportHal importHal;

    public PublicationController(PublicationRepository repository, ImportHal importHal) {
        this.repository = repository;
        this.importHal = importHal;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "publication/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("publication", new Publication());
        return "publication/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("publication", repository.findById(id).orElseThrow());
        return "publication/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("publication", repository.findById(id).orElseThrow());
        return "publication/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("publication") Publication publication, BindingResult result) {
        if (result.hasErrors()) {
            return "publication/form";
        }
        repository.save(publication);
        return "redirect:/publications";
    }

    @PostMapping("/hal")
    public String mettreAJourDepuisHal(RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("message", importHal.importerPourTous().getMessage());
        } catch (RestClientException e) {
            redirectAttributes.addFlashAttribute("erreur", "HAL injoignable : " + e.getMessage());
        }
        return "redirect:/publications";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/publications";
    }
}
