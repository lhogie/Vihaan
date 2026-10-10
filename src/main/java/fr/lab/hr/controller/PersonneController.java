package fr.lab.hr.controller;

import java.util.ArrayList;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.lab.hr.hal.ImportHal;
import fr.lab.hr.model.Personne;
import fr.lab.hr.repository.AdresseRepository;
import fr.lab.hr.repository.BureauRepository;
import fr.lab.hr.repository.PersonneRepository;
import fr.lab.hr.repository.PublicationRepository;
import fr.lab.hr.repository.StructureRepository;

@Controller
@RequestMapping("/personnes")
public class PersonneController {

    private final PersonneRepository repository;
    private final AdresseRepository adresseRepository;
    private final BureauRepository bureauRepository;
    private final StructureRepository structureRepository;
    private final PublicationRepository publicationRepository;
    private final ImportHal importHal;

    public PersonneController(PersonneRepository repository, AdresseRepository adresseRepository,
            BureauRepository bureauRepository, StructureRepository structureRepository,
            PublicationRepository publicationRepository, ImportHal importHal) {
        this.repository = repository;
        this.adresseRepository = adresseRepository;
        this.bureauRepository = bureauRepository;
        this.structureRepository = structureRepository;
        this.publicationRepository = publicationRepository;
        this.importHal = importHal;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "personne/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Personne personne = new Personne();
        model.addAttribute("personne", personne);
        addReferenceLists(model);
        addTextListAttributes(model, personne);
        return "personne/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Personne personne = repository.findById(id).orElseThrow();
        model.addAttribute("personne", personne);
        addReferenceLists(model);
        addTextListAttributes(model, personne);
        return "personne/form";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("personne", repository.findById(id).orElseThrow());
        return "personne/show";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("personne") Personne personne, BindingResult result,
            @RequestParam(required = false) Long adresseId, @RequestParam(required = false) Long bureauPrincipalId,
            @RequestParam(required = false) List<Long> bureauxSecondairesIds,
            @RequestParam(required = false) Long structureId,
            @RequestParam(required = false) List<Long> structuresImpliqueesIds,
            @RequestParam(required = false) List<Long> publicationsIds,
            @RequestParam(required = false) String telephonesTexte, @RequestParam(required = false) String emailsTexte,
            @RequestParam(required = false) String pagesWebTexte, Model model) {
        if (result.hasErrors()) {
            addReferenceLists(model);
            model.addAttribute("telephonesTexte", telephonesTexte);
            model.addAttribute("emailsTexte", emailsTexte);
            model.addAttribute("pagesWebTexte", pagesWebTexte);
            return "personne/form";
        }
        personne.setAdressePostale(adresseId != null ? adresseRepository.findById(adresseId).orElse(null) : null);
        personne.setBureauPrincipal(
                bureauPrincipalId != null ? bureauRepository.findById(bureauPrincipalId).orElse(null) : null);
        personne.setBureauxSecondaires(
                bureauxSecondairesIds != null ? bureauRepository.findAllById(bureauxSecondairesIds)
                        : Collections.emptyList());
        personne.setStructure(structureId != null ? structureRepository.findById(structureId).orElse(null) : null);
        personne.setStructuresImpliquees(
                structuresImpliqueesIds != null ? structureRepository.findAllById(structuresImpliqueesIds)
                        : Collections.emptyList());
        personne.setPublications(
                publicationsIds != null ? publicationRepository.findAllById(publicationsIds) : Collections.emptyList());
        personne.setTelephones(linesToList(telephonesTexte));
        personne.setEmails(linesToList(emailsTexte));
        personne.setPagesWeb(linesToList(pagesWebTexte));
        repository.save(personne);
        return "redirect:/personnes";
    }

    @PostMapping("/{id}/hal")
    public String mettreAJourDepuisHal(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Personne personne = repository.findById(id).orElseThrow();
        if (!ImportHal.aUnIdHal(personne)) {
            redirectAttributes.addFlashAttribute("erreur", "Renseigner l'identifiant HAL de la personne avant l'import.");
        } else {
            try {
                redirectAttributes.addFlashAttribute("message", importHal.importerPourPersonne(id).getMessage());
            } catch (RestClientException e) {
                redirectAttributes.addFlashAttribute("erreur", "HAL injoignable : " + e.getMessage());
            }
        }
        return "redirect:/personnes/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/personnes";
    }

    private void addReferenceLists(Model model) {
        model.addAttribute("adresseList", adresseRepository.findAll());
        model.addAttribute("bureauList", bureauRepository.findAll());
        model.addAttribute("structureList", structureRepository.findAll());
        model.addAttribute("publicationList", publicationRepository.findAll());
    }

    private void addTextListAttributes(Model model, Personne personne) {
        model.addAttribute("telephonesTexte", String.join("\n", personne.getTelephones()));
        model.addAttribute("emailsTexte", String.join("\n", personne.getEmails()));
        model.addAttribute("pagesWebTexte", String.join("\n", personne.getPagesWeb()));
    }

    private List<String> linesToList(String texte) {
        if (texte == null || texte.isBlank()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String ligne : texte.split("\\R")) {
            if (!ligne.isBlank()) {
                result.add(ligne.trim());
            }
        }
        return result;
    }
}
