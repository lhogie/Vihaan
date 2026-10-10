package fr.lab.hr.export;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

/**
 * Un champ d'entité tel qu'il apparaît dans les exports JSON, avec sa clé.
 * Partagé par l'export et la restauration pour qu'ils lisent et écrivent
 * exactement le même format.
 */
record Champ(Field field, Nature nature, String cle) {

    enum Nature {
        /** L'identifiant ({@code @Id}). */
        ID,
        /** Valeur simple : texte, nombre, booléen, date, enum. */
        SIMPLE,
        /** {@code @ManyToOne} / {@code @OneToOne} propriétaire, exporté sous {@code xxxId}. */
        REFERENCE,
        /** Collection d'entités côté propriétaire, exportée sous {@code xxxIds}. */
        COLLECTION,
        /** {@code @ElementCollection} de valeurs simples. */
        VALEURS
    }

    /** Champs exportés de la classe, dans l'ordre de déclaration ; les côtés inverses ({@code mappedBy}) sont exclus. */
    static List<Champ> de(Class<?> classe) {
        List<Champ> champs = new ArrayList<>();
        for (Field field : classe.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || estCoteInverse(field)) {
                continue;
            }
            field.setAccessible(true);
            String nom = field.getName();
            if (field.isAnnotationPresent(Id.class)) {
                champs.add(new Champ(field, Nature.ID, nom));
            } else if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
                champs.add(new Champ(field, Nature.REFERENCE, nom + "Id"));
            } else if (field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(OneToMany.class)) {
                champs.add(new Champ(field, Nature.COLLECTION, nom + "Ids"));
            } else if (field.isAnnotationPresent(ElementCollection.class)) {
                champs.add(new Champ(field, Nature.VALEURS, nom));
            } else {
                champs.add(new Champ(field, Nature.SIMPLE, nom));
            }
        }
        return champs;
    }

    Object lire(Object objet) {
        try {
            return field.get(objet);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean estCoteInverse(Field field) {
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        return (oneToMany != null && !oneToMany.mappedBy().isEmpty())
                || (manyToMany != null && !manyToMany.mappedBy().isEmpty())
                || (oneToOne != null && !oneToOne.mappedBy().isEmpty());
    }
}
