package fr.lab.hr.hal;

import java.util.Set;

/**
 * Une publication telle que renvoyée par HAL. {@code idHalAuteurs} contient
 * les idHal textuels et numériques de tous les auteurs qui en ont un.
 */
public record DocumentHal(String halId, String titre, Integer annee, Set<String> idHalAuteurs) {
}
