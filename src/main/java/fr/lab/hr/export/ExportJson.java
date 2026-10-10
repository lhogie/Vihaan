package fr.lab.hr.export;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Exporte les entités en structures JSON-sérialisables (Map/List), par
 * réflexion sur les champs déclarés, comme {@code GenericSearch} : une nouvelle
 * entité est exportée (et restaurable par {@link ImportJson}) sans code
 * spécifique, il suffit de l'ajouter à {@link #TYPES}.
 * <p>
 * Les relations sont exportées par identifiant, sous le nom utilisé par les
 * formulaires ({@code structureId}, {@code bureauxSecondairesIds}). Seul le
 * côté propriétaire est exporté : les côtés inverses ({@code mappedBy}) s'en
 * déduisent et créeraient des cycles. Voir {@link Champ}.
 */
@Service
public class ExportJson {

    /** Types exportés, indexés par le préfixe d'URL de leur contrôleur (/export/json/{type}). */
    static final Map<String, Class<? extends Identifiable>> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("personnes", Personne.class);
        TYPES.put("structures", Structure.class);
        TYPES.put("contrats", Contrat.class);
        TYPES.put("employeurs", Employeur.class);
        TYPES.put("grades", Grade.class);
        TYPES.put("postes", Poste.class);
        TYPES.put("bureaux", Bureau.class);
        TYPES.put("batiments", Batiment.class);
        TYPES.put("campus", Campus.class);
        TYPES.put("adresses", Adresse.class);
        TYPES.put("pays", Pays.class);
        TYPES.put("publications", Publication.class);
    }

    private final EntityManager entityManager;

    public ExportJson(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> tout() {
        Map<String, Object> export = new LinkedHashMap<>();
        TYPES.keySet().forEach(type -> export.put(type, parType(type).orElseThrow()));
        return export;
    }

    @Transactional(readOnly = true)
    public Optional<List<Map<String, Object>>> parType(String type) {
        Class<? extends Identifiable> classe = TYPES.get(type);
        if (classe == null) {
            return Optional.empty();
        }
        List<? extends Identifiable> entites = entityManager
                .createQuery("select e from " + classe.getSimpleName() + " e", classe).getResultList();
        return Optional.of(entites.stream()
                .sorted(Comparator.comparing(Identifiable::getId))
                .map(ExportJson::versMap)
                .toList());
    }

    static Map<String, Object> versMap(Object entite) {
        Object objet = Hibernate.unproxy(entite);
        Map<String, Object> map = new LinkedHashMap<>();
        for (Champ champ : Champ.de(objet.getClass())) {
            Object valeur = champ.lire(objet);
            switch (champ.nature()) {
                case REFERENCE -> map.put(champ.cle(), valeur == null ? null : ((Identifiable) valeur).getId());
                case COLLECTION -> {
                    List<Long> ids = new ArrayList<>();
                    ((Collection<?>) valeur).forEach(e -> ids.add(((Identifiable) e).getId()));
                    ids.sort(Comparator.naturalOrder());
                    map.put(champ.cle(), ids);
                }
                case VALEURS -> map.put(champ.cle(), new ArrayList<>((Collection<?>) valeur));
                case ID, SIMPLE -> map.put(champ.cle(), simple(valeur));
            }
        }
        return map;
    }

    private static Object simple(Object valeur) {
        if (valeur instanceof Temporal || valeur instanceof Enum<?>) {
            return valeur.toString();
        }
        return valeur;
    }
}
