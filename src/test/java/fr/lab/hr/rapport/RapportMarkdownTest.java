package fr.lab.hr.rapport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.lab.hr.model.Batiment;
import fr.lab.hr.model.Bureau;
import fr.lab.hr.model.Employeur;
import fr.lab.hr.model.Grade;
import fr.lab.hr.model.Personne;
import fr.lab.hr.model.Poste;
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

class RapportMarkdownTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 9);

    private PersonneRepository personneRepository;
    private StructureRepository structureRepository;
    private PosteRepository posteRepository;
    private BureauRepository bureauRepository;
    private RapportMarkdown rapport;

    @BeforeEach
    void setUp() {
        personneRepository = mock(PersonneRepository.class);
        structureRepository = mock(StructureRepository.class);
        posteRepository = mock(PosteRepository.class);
        bureauRepository = mock(BureauRepository.class);
        rapport = new RapportMarkdown(personneRepository, structureRepository, mock(ContratRepository.class),
                mock(EmployeurRepository.class), mock(GradeRepository.class), posteRepository, bureauRepository,
                mock(BatimentRepository.class), mock(CampusRepository.class), mock(AdresseRepository.class),
                mock(PaysRepository.class), mock(PublicationRepository.class));
    }

    @Test
    void unPosteEstEnCoursEntreSesDatesIncluses() {
        assertTrue(RapportMarkdown.estEnCours(poste(null, null), AUJOURDHUI));
        assertTrue(RapportMarkdown.estEnCours(poste(AUJOURDHUI, AUJOURDHUI), AUJOURDHUI));
        assertFalse(RapportMarkdown.estEnCours(poste(null, AUJOURDHUI.minusDays(1)), AUJOURDHUI));
        assertFalse(RapportMarkdown.estEnCours(poste(AUJOURDHUI.plusDays(1), null), AUJOURDHUI));
    }

    @Test
    void organigrammeAnnuaireEtStatistiquesUtilisentLePosteEnCours() {
        Structure labo = structure(1L, "I3S", null);
        Structure pole = structure(2L, "SPARKS", labo);
        Structure equipe = structure(3L, "WIMMICS", pole);
        Batiment batiment = new Batiment();
        batiment.setNom("TEMPLIERS");
        Bureau bureau = new Bureau();
        bureau.setId(1L);
        bureau.setNom("426");
        bureau.setCapacite(1);
        bureau.setBatiment(batiment);

        Personne alice = personne(1L, "Martin", "Alice", equipe);
        alice.setBureauPrincipal(bureau);
        alice.getEmails().add("alice@i3s.fr");
        Personne bob = personne(2L, "Durand", "Bob", equipe);
        bob.setBureauPrincipal(bureau);
        Personne chef = personne(3L, "Petit", "Chef", pole);

        Grade dr = grade("DR1");
        Grade cr = grade("CR1");
        Employeur cnrs = new Employeur();
        cnrs.setNom("CNRS");
        Poste ancien = poste(LocalDate.of(2010, 1, 1), LocalDate.of(2015, 12, 31));
        ancien.setPersonne(alice);
        ancien.setGrade(cr);
        ancien.setEmployeur(cnrs);
        Poste actuel = poste(LocalDate.of(2016, 1, 1), null);
        actuel.setPersonne(alice);
        actuel.setGrade(dr);
        actuel.setEmployeur(cnrs);
        Poste termine = poste(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));
        termine.setPersonne(bob);
        termine.setGrade(cr);

        when(personneRepository.findAll()).thenReturn(List.of(alice, bob, chef));
        when(structureRepository.findAll()).thenReturn(List.of(labo, pole, equipe));
        when(posteRepository.findAll()).thenReturn(List.of(ancien, actuel, termine));
        when(bureauRepository.findAll()).thenReturn(List.of(bureau));

        String md = rapport.generer(AUJOURDHUI);

        assertTrue(md.contains("- **I3S** : 0 (3)\n  - **SPARKS** : 1 (3)\n    - **WIMMICS** : 2\n"), md);
        assertTrue(md.contains("| DR1 - Directeur | 1 |"), md);
        assertFalse(md.contains("| CR1 - Directeur | "), "un poste terminé ne compte pas");
        assertTrue(md.contains("| Martin | Alice | DR1 | CNRS | TEMPLIERS 426 | alice@i3s.fr |"), md);
        assertTrue(md.contains("| Durand | Bob | — | — | TEMPLIERS 426 | — |"), md);
        assertTrue(md.contains("### I3S › SPARKS › WIMMICS (2)"), md);
        assertTrue(md.contains("| Personnes dont tous les postes sont terminés | 1 |"), md);
        assertTrue(md.contains("Personnes sans aucun poste : Chef Petit."), md);
        assertTrue(md.contains("- TEMPLIERS 426 (2 pour 1)"), md);
    }

    @Test
    void echappeLesCaracteresQuiCasseraientUnTableau() {
        assertEquals("C&A \\| B\\_x \\*a\\* l1 l2", RapportMarkdown.echapper("C&A | B_x *a* l1\nl2"));
    }

    private static Structure structure(Long id, String nom, Structure parente) {
        Structure s = new Structure();
        s.setId(id);
        s.setNom(nom);
        s.setStructureParente(parente);
        if (parente != null) {
            parente.getSousStructures().add(s);
        }
        return s;
    }

    private static Personne personne(Long id, String nom, String prenom, Structure structure) {
        Personne p = new Personne();
        p.setId(id);
        p.setNom(nom);
        p.setPrenom(prenom);
        p.setStructure(structure);
        return p;
    }

    private static Grade grade(String code) {
        Grade g = new Grade();
        g.setCode(code);
        g.setLibelle("Directeur");
        return g;
    }

    private static Poste poste(LocalDate debut, LocalDate fin) {
        Poste p = new Poste();
        p.setDateDebut(debut);
        p.setDateFin(fin);
        return p;
    }
}
