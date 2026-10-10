package fr.lab.hr.securite;

/**
 * Rôles de l'application. Un gestionnaire a aussi tous les droits d'un
 * lecteur (hiérarchie déclarée dans {@link ConfigurationSecurite}).
 */
public enum Role {

    /** Consulte toutes les fiches, la recherche et le rapport Markdown. */
    LECTEUR,

    /** Modifie les données : création, modification, suppression, import HAL, exports JSON, restauration, vidage. */
    GESTIONNAIRE
}
