package fr.lab.hr.controller;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import fr.lab.hr.config.DataSeeder;
import fr.lab.hr.export.ImportJson;
import fr.lab.hr.repository.GradeRepository;
import fr.lab.hr.repository.PaysRepository;

@Controller
public class ImportController {

    /** Mot à saisir pour confirmer « Vider la base ». */
    static final String CONFIRMATION_VIDER = "VIDER";

    private final ImportJson importJson;
    private final JsonMapper jsonMapper;
    private final DataSeeder dataSeeder;
    private final PaysRepository paysRepository;
    private final GradeRepository gradeRepository;

    public ImportController(ImportJson importJson, JsonMapper jsonMapper, DataSeeder dataSeeder,
            PaysRepository paysRepository, GradeRepository gradeRepository) {
        this.importJson = importJson;
        this.jsonMapper = jsonMapper;
        this.dataSeeder = dataSeeder;
        this.paysRepository = paysRepository;
        this.gradeRepository = gradeRepository;
    }

    @GetMapping("/vider")
    public String confirmerVider() {
        return "sauvegarde/vider";
    }

    @PostMapping("/vider")
    public String vider(@RequestParam(required = false) String confirmation, RedirectAttributes redirectAttributes) {
        if (!CONFIRMATION_VIDER.equals(confirmation == null ? null : confirmation.trim())) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Saisir « " + CONFIRMATION_VIDER + " » pour confirmer. La base n'a pas été modifiée.");
            return "redirect:/vider";
        }
        importJson.vider();
        dataSeeder.peupler();
        redirectAttributes.addFlashAttribute("message", "Base vidée. Tables de référence repeuplées : "
                + paysRepository.count() + " pays, " + gradeRepository.count() + " grades.");
        return "redirect:/personnes";
    }

    @GetMapping("/import")
    public String formulaire() {
        return "sauvegarde/restaurer";
    }

    @PostMapping("/import/json")
    public String restaurer(@RequestParam MultipartFile fichier, RedirectAttributes redirectAttributes) {
        if (fichier.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "Choisir un fichier d'export JSON.");
            return "redirect:/import";
        }
        try {
            Object contenu = jsonMapper.readValue(fichier.getBytes(), Object.class);
            if (!(contenu instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("Ce fichier n'est pas un export complet de la base (c'est une liste, "
                        + "probablement l'export d'un seul type). Utiliser le fichier produit par « Export JSON » de la barre latérale.");
            }
            Map<String, Object> donnees = jsonMapper.convertValue(contenu, new TypeReference<>() {
            });
            Map<String, Integer> bilan = importJson.restaurer(donnees);
            redirectAttributes.addFlashAttribute("message", "Base restaurée depuis « " + fichier.getOriginalFilename()
                    + " » : " + bilan.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
                            .collect(Collectors.joining(", ")) + ".");
            return "redirect:/personnes";
        } catch (IOException | JacksonException e) {
            redirectAttributes.addFlashAttribute("erreur", "Fichier illisible : ce n'est pas un JSON valide.");
        } catch (IllegalArgumentException | ClassCastException e) {
            redirectAttributes.addFlashAttribute("erreur", "Fichier refusé, la base n'a pas été modifiée. " + e.getMessage());
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Restauration impossible, la base n'a pas été modifiée : "
                            + e.getMostSpecificCause().getMessage());
        }
        return "redirect:/import";
    }
}
