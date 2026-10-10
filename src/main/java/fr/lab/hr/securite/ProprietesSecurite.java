package fr.lab.hr.securite;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Comptes locaux, déclarés sous {@code securite.utilisateurs} dans application.yml.
 * Les mots de passe portent le préfixe de leur encodage : {@code {bcrypt}…}
 * en production, {@code {noop}…} (en clair) seulement en développement.
 */
@ConfigurationProperties("securite")
public record ProprietesSecurite(List<Utilisateur> utilisateurs) {

    public ProprietesSecurite {
        utilisateurs = utilisateurs == null ? List.of() : List.copyOf(utilisateurs);
    }

    public record Utilisateur(String identifiant, String motDePasse, Role role) {
    }
}
