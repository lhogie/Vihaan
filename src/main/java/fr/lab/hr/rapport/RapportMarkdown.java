package fr.lab.hr.rapport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.lab.hr.model.Batiment;
import fr.lab.hr.model.Bureau;
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

/**
 * Construit un rapport Markdown de l'état de la base : volumes, organigramme,
 * répartition des postes en cours, bureaux, qualité des données et annuaire.
 * Un poste est « en cours » à une date s'il a commencé (ou n'a pas de début)
 * et n'est pas terminé (ou n'a pas de fin).
 */
@Service
public class RapportMarkdown {

    private static final String SANS = "—";

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

    public RapportMarkdown(PersonneRepository personneRepository, StructureRepository structureRepository,
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

    @Transactional(readOnly = true)
    public String generer(LocalDate aujourdhui) {
        List<Personne> personnes = new ArrayList<>(personneRepository.findAll());
        personnes.sort(Comparator.comparing((Personne p) -> p.getNom().toUpperCase())
                .thenComparing(p -> p.getPrenom().toUpperCase()));
        List<Structure> structures = structureRepository.findAll();
        List<Poste> postes = posteRepository.findAll();

        Map<Long, Poste> posteEnCours = new HashMap<>();
        Map<Long, Integer> nbPostes = new HashMap<>();
        for (Poste poste : postes) {
            if (poste.getPersonne() == null) {
                continue;
            }
            Long id = poste.getPersonne().getId();
            nbPostes.merge(id, 1, Integer::sum);
            if (estEnCours(poste, aujourdhui)) {
                posteEnCours.merge(id, poste, RapportMarkdown::plusRecent);
            }
        }

        StringBuilder md = new StringBuilder();
        md.append("# Rapport de la base RH\n\n");
        md.append("Généré le ").append(aujourdhui).append(".\n\n");

        synthese(md);
        organigramme(md, structures, personnes);
        repartition(md, "Postes en cours par grade", "Grade",
                posteEnCours.values(), p -> p.getGrade() == null ? null : p.getGrade().toString());
        repartition(md, "Postes en cours par employeur", "Employeur",
                posteEnCours.values(), p -> p.getEmployeur() == null ? null : p.getEmployeur().getNom());
        bureaux(md, personnes);
        qualite(md, personnes, postes, posteEnCours, nbPostes);
        annuaire(md, personnes, posteEnCours);
        return md.toString();
    }

    static boolean estEnCours(Poste poste, LocalDate jour) {
        return (poste.getDateDebut() == null || !poste.getDateDebut().isAfter(jour))
                && (poste.getDateFin() == null || !poste.getDateFin().isBefore(jour));
    }

    private static Poste plusRecent(Poste a, Poste b) {
        if (a.getDateDebut() == null) {
            return b;
        }
        if (b.getDateDebut() == null) {
            return a;
        }
        return b.getDateDebut().isAfter(a.getDateDebut()) ? b : a;
    }

    private void synthese(StringBuilder md) {
        md.append("## Synthèse\n\n");
        tableau(md, List.of("Entité", "Nombre"), List.of(
                List.of("Personnes", String.valueOf(personneRepository.count())),
                List.of("Structures", String.valueOf(structureRepository.count())),
                List.of("Postes", String.valueOf(posteRepository.count())),
                List.of("Employeurs", String.valueOf(employeurRepository.count())),
                List.of("Grades", String.valueOf(gradeRepository.count())),
                List.of("Contrats", String.valueOf(contratRepository.count())),
                List.of("Publications", String.valueOf(publicationRepository.count())),
                List.of("Campus", String.valueOf(campusRepository.count())),
                List.of("Bâtiments", String.valueOf(batimentRepository.count())),
                List.of("Bureaux", String.valueOf(bureauRepository.count())),
                List.of("Adresses", String.valueOf(adresseRepository.count())),
                List.of("Pays", String.valueOf(paysRepository.count()))));
    }

    private void organigramme(StringBuilder md, List<Structure> structures, List<Personne> personnes) {
        md.append("## Structures\n\n");
        if (structures.isEmpty()) {
            md.append("Aucune structure.\n\n");
            return;
        }
        Map<Long, Long> effectifs = personnes.stream().filter(p -> p.getStructure() != null)
                .collect(Collectors.groupingBy(p -> p.getStructure().getId(), Collectors.counting()));
        md.append("Effectif direct de chaque structure, et entre parenthèses l'effectif total en incluant ses sous-structures.\n\n");
        structures.stream().filter(s -> s.getStructureParente() == null)
                .sorted(Comparator.comparing(Structure::getNom, String.CASE_INSENSITIVE_ORDER))
                .forEach(s -> branche(md, s, effectifs, 0));
        md.append('\n');
    }

    private long branche(StringBuilder md, Structure structure, Map<Long, Long> effectifs, int niveau) {
        StringBuilder sousArbre = new StringBuilder();
        long total = effectifs.getOrDefault(structure.getId(), 0L);
        List<Structure> enfants = structure.getSousStructures().stream()
                .sorted(Comparator.comparing(Structure::getNom, String.CASE_INSENSITIVE_ORDER)).toList();
        for (Structure enfant : enfants) {
            total += branche(sousArbre, enfant, effectifs, niveau + 1);
        }
        String ligne = "  ".repeat(niveau) + "- **" + echapper(structure.getNom()) + "** : "
                + effectifs.getOrDefault(structure.getId(), 0L)
                + (enfants.isEmpty() ? "" : " (" + total + ")") + "\n";
        md.append(ligne).append(sousArbre);
        return total;
    }

    private void repartition(StringBuilder md, String titre, String colonne, Iterable<Poste> postes,
            Function<Poste, String> cle) {
        md.append("## ").append(titre).append("\n\n");
        Map<String, Integer> compte = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Poste poste : postes) {
            compte.merge(Objects.requireNonNullElse(cle.apply(poste), "(non renseigné)"), 1, Integer::sum);
        }
        if (compte.isEmpty()) {
            md.append("Aucun poste en cours.\n\n");
            return;
        }
        List<List<String>> lignes = compte.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> List.of(e.getKey(), String.valueOf(e.getValue()))).toList();
        tableau(md, List.of(colonne, "Personnes"), lignes);
    }

    private void bureaux(StringBuilder md, List<Personne> personnes) {
        md.append("## Bureaux\n\n");
        Map<Long, Long> occupants = personnes.stream().filter(p -> p.getBureauPrincipal() != null)
                .collect(Collectors.groupingBy(p -> p.getBureauPrincipal().getId(), Collectors.counting()));
        Map<String, List<Bureau>> parBatiment = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Bureau bureau : bureauRepository.findAll()) {
            Batiment batiment = bureau.getBatiment();
            parBatiment.computeIfAbsent(batiment == null ? "(sans bâtiment)" : batiment.getNom(),
                    k -> new ArrayList<>()).add(bureau);
        }
        if (parBatiment.isEmpty()) {
            md.append("Aucun bureau.\n\n");
            return;
        }
        List<List<String>> lignes = new ArrayList<>();
        List<String> surOccupes = new ArrayList<>();
        for (Map.Entry<String, List<Bureau>> e : parBatiment.entrySet()) {
            long occupes = 0;
            long total = 0;
            for (Bureau bureau : e.getValue()) {
                long n = occupants.getOrDefault(bureau.getId(), 0L);
                occupes += n > 0 ? 1 : 0;
                total += n;
                if (bureau.getCapacite() != null && n > bureau.getCapacite()) {
                    surOccupes.add(e.getKey() + " " + bureau.getNom() + " (" + n + " pour " + bureau.getCapacite() + ")");
                }
            }
            lignes.add(List.of(e.getKey(), String.valueOf(e.getValue().size()), String.valueOf(occupes),
                    String.valueOf(total)));
        }
        tableau(md, List.of("Bâtiment", "Bureaux", "Bureaux occupés", "Occupants (bureau principal)"), lignes);
        if (!surOccupes.isEmpty()) {
            md.append("Bureaux au-delà de leur capacité :\n\n");
            surOccupes.forEach(s -> md.append("- ").append(echapper(s)).append('\n'));
            md.append('\n');
        }
    }

    private void qualite(StringBuilder md, List<Personne> personnes, List<Poste> postes,
            Map<Long, Poste> posteEnCours, Map<Long, Integer> nbPostes) {
        md.append("## Qualité des données\n\n");
        Map<String, List<Personne>> controles = new LinkedHashMap<>();
        controles.put("Personnes sans structure", personnes.stream().filter(p -> p.getStructure() == null).toList());
        controles.put("Personnes sans e-mail", personnes.stream().filter(p -> p.getEmails().isEmpty()).toList());
        controles.put("Personnes sans aucun poste",
                personnes.stream().filter(p -> !nbPostes.containsKey(p.getId())).toList());
        controles.put("Personnes dont tous les postes sont terminés", personnes.stream()
                .filter(p -> nbPostes.containsKey(p.getId()) && !posteEnCours.containsKey(p.getId())).toList());
        List<List<String>> lignes = new ArrayList<>();
        controles.forEach((libelle, liste) -> lignes.add(List.of(libelle, String.valueOf(liste.size()))));
        lignes.add(List.of("Postes sans grade",
                String.valueOf(postes.stream().filter(p -> p.getGrade() == null).count())));
        lignes.add(List.of("Postes sans employeur",
                String.valueOf(postes.stream().filter(p -> p.getEmployeur() == null).count())));
        List<Publication> publications = publicationRepository.findAll();
        lignes.add(List.of("Publications sans auteur du labo",
                String.valueOf(publications.stream().filter(p -> p.getAuteurs().isEmpty()).count())));
        tableau(md, List.of("Contrôle", "Nombre"), lignes);

        for (String libelle : List.of("Personnes sans structure", "Personnes sans aucun poste")) {
            List<Personne> liste = controles.get(libelle);
            if (!liste.isEmpty() && liste.size() <= 20) {
                md.append(libelle).append(" : ")
                        .append(liste.stream().map(p -> echapper(p.getNomComplet())).collect(Collectors.joining(", ")))
                        .append(".\n\n");
            }
        }
    }

    private void annuaire(StringBuilder md, List<Personne> personnes, Map<Long, Poste> posteEnCours) {
        md.append("## Annuaire\n\n");
        if (personnes.isEmpty()) {
            md.append("Aucune personne.\n");
            return;
        }
        md.append("Grade et employeur du poste en cours le plus récent ; « ").append(SANS)
                .append(" » si aucun poste n'est en cours.\n\n");
        Map<String, List<Personne>> parStructure = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Personne p : personnes) {
            parStructure.computeIfAbsent(p.getStructure() == null ? "(sans structure)" : chemin(p.getStructure()),
                    k -> new ArrayList<>()).add(p);
        }
        for (Map.Entry<String, List<Personne>> e : parStructure.entrySet()) {
            md.append("### ").append(echapper(e.getKey())).append(" (").append(e.getValue().size()).append(")\n\n");
            List<List<String>> lignes = new ArrayList<>();
            for (Personne p : e.getValue()) {
                Poste poste = posteEnCours.get(p.getId());
                lignes.add(List.of(
                        p.getNom(),
                        p.getPrenomUsage() != null && !p.getPrenomUsage().isBlank() ? p.getPrenomUsage() : p.getPrenom(),
                        poste == null || poste.getGrade() == null ? SANS : poste.getGrade().getCode(),
                        poste == null || poste.getEmployeur() == null ? SANS : poste.getEmployeur().getNom(),
                        p.getBureauPrincipal() == null ? SANS : bureau(p.getBureauPrincipal()),
                        p.getEmails().isEmpty() ? SANS : String.join(", ", p.getEmails())));
            }
            tableau(md, List.of("Nom", "Prénom", "Grade", "Employeur", "Bureau", "E-mail"), lignes);
        }
    }

    private static String chemin(Structure structure) {
        List<String> noms = new ArrayList<>();
        for (Structure s = structure; s != null && noms.size() < 20; s = s.getStructureParente()) {
            noms.add(0, s.getNom());
        }
        return String.join(" › ", noms);
    }

    private static String bureau(Bureau bureau) {
        return bureau.getBatiment() == null ? bureau.getNom() : bureau.getBatiment().getNom() + " " + bureau.getNom();
    }

    private static void tableau(StringBuilder md, List<String> entetes, List<List<String>> lignes) {
        md.append("| ").append(entetes.stream().map(RapportMarkdown::echapper).collect(Collectors.joining(" | ")))
                .append(" |\n|");
        entetes.forEach(h -> md.append(" --- |"));
        md.append('\n');
        for (List<String> ligne : lignes) {
            md.append("| ").append(ligne.stream().map(RapportMarkdown::echapper).collect(Collectors.joining(" | ")))
                    .append(" |\n");
        }
        md.append('\n');
    }

    /** Neutralise les caractères qui casseraient un tableau ou la mise en forme Markdown. */
    static String echapper(String texte) {
        if (texte == null) {
            return "";
        }
        return texte.replace("\\", "\\\\").replace("|", "\\|").replace("*", "\\*").replace("_", "\\_")
                .replace("\r", " ").replace("\n", " ");
    }
}
