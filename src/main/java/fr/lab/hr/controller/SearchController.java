package fr.lab.hr.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.lab.hr.model.Adresse;
import fr.lab.hr.model.Batiment;
import fr.lab.hr.model.Bureau;
import fr.lab.hr.model.Campus;
import fr.lab.hr.model.Contrat;
import fr.lab.hr.model.Employeur;
import fr.lab.hr.model.Grade;
import fr.lab.hr.model.Identifiable;
import fr.lab.hr.model.Pays;
import fr.lab.hr.model.Personne;
import fr.lab.hr.model.Poste;
import fr.lab.hr.model.Publication;
import fr.lab.hr.model.Structure;
import fr.lab.hr.repository.AdresseRepository;
import fr.lab.hr.repository.BatimentRepository;
import fr.lab.hr.repository.BureauRepository;
import fr.lab.hr.repository.CampusRepository;
import fr.lab.hr.repository.ContratRepository;
import fr.lab.hr.repository.EmployeurRepository;
import fr.lab.hr.repository.GradeRepository;
import fr.lab.hr.repository.PaysRepository;
import fr.lab.hr.repository.PersonneRepository;
import fr.lab.hr.repository.PosteRepository;
import fr.lab.hr.repository.PublicationRepository;
import fr.lab.hr.repository.StructureRepository;
import fr.lab.hr.search.GenericSearch;
import fr.lab.hr.search.SearchHit;
import fr.lab.hr.search.SearchResultGroup;

@Controller
public class SearchController {

    private final PersonneRepository personneRepository;
    private final StructureRepository structureRepository;
    private final ContratRepository contratRepository;
    private final EmployeurRepository employeurRepository;
    private final GradeRepository gradeRepository;
    private final PosteRepository posteRepository;
    private final BureauRepository bureauRepository;
    private final BatimentRepository batimentRepository;
    private final CampusRepository campusRepository;
    private final AdresseRepository adresseRepository;
    private final PaysRepository paysRepository;
    private final PublicationRepository publicationRepository;

    public SearchController(PersonneRepository personneRepository, StructureRepository structureRepository,
            ContratRepository contratRepository, EmployeurRepository employeurRepository,
            GradeRepository gradeRepository, PosteRepository posteRepository, BureauRepository bureauRepository,
            BatimentRepository batimentRepository, CampusRepository campusRepository,
            AdresseRepository adresseRepository, PaysRepository paysRepository,
            PublicationRepository publicationRepository) {
        this.personneRepository = personneRepository;
        this.structureRepository = structureRepository;
        this.contratRepository = contratRepository;
        this.employeurRepository = employeurRepository;
        this.gradeRepository = gradeRepository;
        this.posteRepository = posteRepository;
        this.bureauRepository = bureauRepository;
        this.batimentRepository = batimentRepository;
        this.campusRepository = campusRepository;
        this.adresseRepository = adresseRepository;
        this.paysRepository = paysRepository;
        this.publicationRepository = publicationRepository;
    }

    @GetMapping("/recherche")
    public String rechercher(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        List<SearchResultGroup> groupes = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            groupes.add(groupe("Personnes", "personnes", Personne.class, personneRepository, q));
            groupes.add(groupe("Structures", "structures", Structure.class, structureRepository, q));
            groupes.add(groupe("Contrats", "contrats", Contrat.class, contratRepository, q));
            groupes.add(groupe("Employeurs", "employeurs", Employeur.class, employeurRepository, q));
            groupes.add(groupe("Grades", "grades", Grade.class, gradeRepository, q));
            groupes.add(groupe("Postes", "postes", Poste.class, posteRepository, q));
            groupes.add(groupe("Bureaux", "bureaux", Bureau.class, bureauRepository, q));
            groupes.add(groupe("Batiments", "batiments", Batiment.class, batimentRepository, q));
            groupes.add(groupe("Campus", "campus", Campus.class, campusRepository, q));
            groupes.add(groupe("Adresses", "adresses", Adresse.class, adresseRepository, q));
            groupes.add(groupe("Pays", "pays", Pays.class, paysRepository, q));
            groupes.add(groupe("Publications", "publications", Publication.class, publicationRepository, q));
            groupes.removeIf(g -> g.hits().isEmpty());
        }
        model.addAttribute("groupes", groupes);
        return "search/results";
    }

    private <T extends Identifiable> SearchResultGroup groupe(String label, String urlPrefix, Class<T> entityClass,
            org.springframework.data.jpa.repository.JpaSpecificationExecutor<T> repository, String q) {
        List<T> resultats = repository.findAll(GenericSearch.containing(entityClass, q));
        List<SearchHit> hits = new ArrayList<>();
        for (T r : resultats) {
            hits.add(new SearchHit(r.getId(), r.toString()));
        }
        return new SearchResultGroup(label, urlPrefix, hits);
    }
}
