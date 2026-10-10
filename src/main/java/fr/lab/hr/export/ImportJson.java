package fr.lab.hr.export;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.lab.hr.model.Identifiable;

/**
 * Restaure toute la base à partir d'un export JSON complet ({@link ExportJson#tout()}).
 * <p>
 * Le contenu actuel est entièrement remplacé et les identifiants du fichier
 * sont conservés (les liens et URL restent ceux de la sauvegarde). Tout se fait
 * dans une seule transaction : en cas d'erreur (fichier incomplet, référence
 * vers un identifiant absent...), la base n'est pas modifiée.
 * <p>
 * Les écritures passent par JDBC, car JPA ne permet pas d'insérer une entité
 * avec un identifiant {@code IDENTITY} imposé ; les noms de tables et de
 * colonnes viennent du mapping Hibernate, donc rien n'est écrit en dur.
 * Ordre : les références ({@code xxxId}) sont mises à NULL avant de vider les
 * tables, et posées après l'insertion de toutes les lignes, pour que les
 * clés étrangères restent vérifiées sans dépendre de l'ordre des types.
 */
@Service
public class ImportJson {

    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    public ImportJson(EntityManager entityManager, DataSource dataSource) {
        this.entityManager = entityManager;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /** Renvoie le nombre de lignes restaurées par type. */
    @Transactional
    public Map<String, Integer> restaurer(Map<String, Object> donnees) {
        Map<String, List<Map<String, Object>>> lignes = verifier(donnees);
        entityManager.flush();
        entityManager.clear();
        MappingMetamodel metamodel = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class).getMappingMetamodel();

        List<Table> tables = new ArrayList<>();
        ExportJson.TYPES.forEach((type, classe) -> tables.add(new Table(type, classe, metamodel)));

        for (Table table : tables) {
            table.annulerReferences(jdbc);
        }
        for (Table table : tables) {
            table.viderCollections(jdbc);
        }
        for (Table table : tables) {
            jdbc.update("DELETE FROM " + table.nom);
        }
        for (Table table : tables) {
            table.inserer(jdbc, lignes.get(table.type));
        }
        for (Table table : tables) {
            table.poserReferences(jdbc, lignes.get(table.type));
            table.remplirCollections(jdbc, lignes.get(table.type));
        }

        Map<String, Integer> bilan = new LinkedHashMap<>();
        lignes.forEach((type, liste) -> bilan.put(type, liste.size()));
        return bilan;
    }

    /** Supprime toutes les données : restauration d'une sauvegarde où chaque type est vide. */
    @Transactional
    public void vider() {
        Map<String, Object> vide = new LinkedHashMap<>();
        ExportJson.TYPES.keySet().forEach(type -> vide.put(type, List.of()));
        restaurer(vide);
    }

    /** Vérifie que le fichier est un export complet : sinon une restauration viderait les types absents. */
    @SuppressWarnings("unchecked")
    static Map<String, List<Map<String, Object>>> verifier(Map<String, Object> donnees) {
        Map<String, List<Map<String, Object>>> lignes = new LinkedHashMap<>();
        List<String> manquants = new ArrayList<>();
        for (String type : ExportJson.TYPES.keySet()) {
            if (donnees.get(type) instanceof List<?> liste) {
                for (Object ligne : liste) {
                    if (!(ligne instanceof Map<?, ?> map) || !(map.get("id") instanceof Number)) {
                        throw new IllegalArgumentException("Élément sans identifiant dans « " + type + " ».");
                    }
                }
                lignes.put(type, (List<Map<String, Object>>) liste);
            } else {
                manquants.add(type);
            }
        }
        if (!manquants.isEmpty()) {
            throw new IllegalArgumentException("Ce fichier n'est pas un export complet de la base (absents : "
                    + String.join(", ", manquants) + "). Utiliser le fichier produit par « Export JSON » de la barre latérale.");
        }
        return lignes;
    }

    /** Convertit une valeur JSON vers le type Java du champ, pour JDBC. */
    static Object convertir(Object valeur, Class<?> type) {
        if (valeur == null) {
            return type == boolean.class ? Boolean.FALSE : null;
        }
        if (type == LocalDate.class) {
            return LocalDate.parse(valeur.toString());
        }
        if (type.isEnum()) {
            return Enum.valueOf(type.asSubclass(Enum.class), valeur.toString()).name();
        }
        if (type == Integer.class || type == int.class) {
            return ((Number) valeur).intValue();
        }
        if (type == Long.class || type == long.class) {
            return ((Number) valeur).longValue();
        }
        if (type == Double.class || type == double.class) {
            return ((Number) valeur).doubleValue();
        }
        if (type == Boolean.class || type == boolean.class) {
            return (Boolean) valeur;
        }
        if (type == String.class) {
            return valeur.toString();
        }
        throw new IllegalArgumentException("Type de champ non géré à la restauration : " + type.getName());
    }

    private static Long id(Object valeur) {
        return valeur == null ? null : ((Number) valeur).longValue();
    }

    /** Une table d'entité et ses colonnes, déduites du mapping Hibernate. */
    private static final class Table {

        final String type;
        final String nom;
        final String colonneId;
        final AbstractEntityPersister persister;
        final List<Champ> simples = new ArrayList<>();
        final Map<Champ, String> references = new LinkedHashMap<>();
        final Map<Champ, AbstractCollectionPersister> collections = new LinkedHashMap<>();

        Table(String type, Class<? extends Identifiable> classe, MappingMetamodel metamodel) {
            this.type = type;
            this.persister = (AbstractEntityPersister) metamodel.getEntityDescriptor(classe);
            this.nom = persister.getIdentifierTableName();
            this.colonneId = persister.getIdentifierColumnNames()[0];
            for (Champ champ : Champ.de(classe)) {
                String propriete = champ.field().getName();
                switch (champ.nature()) {
                    case SIMPLE -> simples.add(champ);
                    case REFERENCE -> references.put(champ, persister.getPropertyColumnNames(propriete)[0]);
                    case COLLECTION, VALEURS -> collections.put(champ, (AbstractCollectionPersister) metamodel
                            .getCollectionDescriptor(classe.getName() + "." + propriete));
                    case ID -> {
                    }
                }
            }
        }

        void annulerReferences(JdbcTemplate jdbc) {
            for (String colonne : references.values()) {
                jdbc.update("UPDATE " + nom + " SET " + colonne + " = NULL");
            }
        }

        void viderCollections(JdbcTemplate jdbc) {
            for (AbstractCollectionPersister collection : collections.values()) {
                jdbc.update("DELETE FROM " + collection.getTableName());
            }
        }

        void inserer(JdbcTemplate jdbc, List<Map<String, Object>> lignes) {
            List<String> colonnes = new ArrayList<>();
            colonnes.add(colonneId);
            simples.forEach(c -> colonnes.add(persister.getPropertyColumnNames(c.field().getName())[0]));
            String sql = "INSERT INTO " + nom + " (" + String.join(", ", colonnes) + ") VALUES ("
                    + colonnes.stream().map(c -> "?").collect(Collectors.joining(", ")) + ")";
            List<Object[]> valeurs = new ArrayList<>();
            for (Map<String, Object> ligne : lignes) {
                Object[] v = new Object[colonnes.size()];
                v[0] = id(ligne.get("id"));
                for (int i = 0; i < simples.size(); i++) {
                    Champ champ = simples.get(i);
                    v[i + 1] = convertir(ligne.get(champ.cle()), champ.field().getType());
                }
                valeurs.add(v);
            }
            jdbc.batchUpdate(sql, valeurs);
        }

        void poserReferences(JdbcTemplate jdbc, List<Map<String, Object>> lignes) {
            for (Map.Entry<Champ, String> reference : references.entrySet()) {
                String sql = "UPDATE " + nom + " SET " + reference.getValue() + " = ? WHERE " + colonneId + " = ?";
                List<Object[]> valeurs = new ArrayList<>();
                for (Map<String, Object> ligne : lignes) {
                    Long cible = id(ligne.get(reference.getKey().cle()));
                    if (cible != null) {
                        valeurs.add(new Object[] {cible, id(ligne.get("id"))});
                    }
                }
                jdbc.batchUpdate(sql, valeurs);
            }
        }

        void remplirCollections(JdbcTemplate jdbc, List<Map<String, Object>> lignes) {
            for (Map.Entry<Champ, AbstractCollectionPersister> entree : collections.entrySet()) {
                AbstractCollectionPersister collection = entree.getValue();
                String sql = "INSERT INTO " + collection.getTableName() + " (" + collection.getKeyColumnNames()[0]
                        + ", " + collection.getElementColumnNames()[0] + ") VALUES (?, ?)";
                Champ champ = entree.getKey();
                List<Object[]> valeurs = new ArrayList<>();
                for (Map<String, Object> ligne : lignes) {
                    Object elements = ligne.get(champ.cle());
                    if (elements == null) {
                        continue;
                    }
                    for (Object element : (List<?>) elements) {
                        Object valeur = champ.nature() == Champ.Nature.COLLECTION ? id(element) : element.toString();
                        valeurs.add(new Object[] {id(ligne.get("id")), valeur});
                    }
                }
                jdbc.batchUpdate(sql, valeurs);
            }
        }
    }
}
