package fr.lab.hr.hal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bilan d'un import HAL, affiché à l'utilisateur après la mise à jour.
 */
public class ResultatImportHal {

    final Set<String> publicationsCreees = new HashSet<>();
    final Set<String> publicationsExistantes = new HashSet<>();
    final List<String> personnesEnEchec = new ArrayList<>();
    int personnesTraitees;
    int liensAjoutes;

    public int getPublicationsCreees() {
        return publicationsCreees.size();
    }

    public int getPublicationsExistantes() {
        return publicationsExistantes.size();
    }

    public List<String> getPersonnesEnEchec() {
        return personnesEnEchec;
    }

    public int getPersonnesTraitees() {
        return personnesTraitees;
    }

    public int getLiensAjoutes() {
        return liensAjoutes;
    }

    public String getMessage() {
        String message = "Import HAL : " + personnesTraitees + " personne(s) traitée(s), "
                + getPublicationsCreees() + " publication(s) créée(s), "
                + getPublicationsExistantes() + " déjà présente(s) mise(s) à jour, "
                + liensAjoutes + " lien(s) auteur ajouté(s).";
        if (!personnesEnEchec.isEmpty()) {
            message += " Échec pour : " + String.join(", ", personnesEnEchec) + ".";
        }
        return message;
    }
}
