package fr.lab.hr.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import fr.lab.hr.securite.Role;

/**
 * Expose aux templates le chemin de la requête (rubrique courante de la barre
 * latérale), l'utilisateur connecté et ses droits. {@code peutModifier} ne sert
 * qu'à masquer les boutons inutiles pour un lecteur : les droits eux-mêmes sont
 * vérifiés par {@code ConfigurationSecurite}.
 */
@ControllerAdvice
public class NavigationAdvice {

    @ModelAttribute("cheminCourant")
    public String cheminCourant(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("utilisateurCourant")
    public String utilisateurCourant(HttpServletRequest request) {
        return request.getRemoteUser();
    }

    @ModelAttribute("peutModifier")
    public boolean peutModifier(HttpServletRequest request) {
        return request.isUserInRole(Role.GESTIONNAIRE.name());
    }
}
