package fr.lab.hr.search;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Builds a JPA Specification that matches an entity if any of its direct
 * String fields contains the given search term (case-insensitive), so a
 * search query works across every entity without a per-entity query.
 */
public final class GenericSearch {

    private GenericSearch() {
    }

    public static <T> Specification<T> containing(Class<T> entityClass, String term) {
        String pattern = "%" + term.toLowerCase() + "%";
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.getType().equals(String.class)) {
                    predicates.add(cb.like(cb.lower(root.get(field.getName())), pattern));
                }
            }
            if (predicates.isEmpty()) {
                return cb.disjunction();
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
