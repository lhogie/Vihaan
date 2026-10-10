package fr.lab.hr.hal;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import fr.lab.hr.model.Personne;
import fr.lab.hr.model.Publication;
import fr.lab.hr.repository.PersonneRepository;
import fr.lab.hr.repository.PublicationRepository;

/**
 * Met à jour les publications à partir de HAL. Une publication est identifiée
 * par son idHal (pas de doublon) ; tout membre du labo dont l'idHal figure
 * parmi les auteurs HAL est lié à la publication, côté {@link Personne}
 * (propriétaire de la relation).
 */
@Service
public class ImportHal {

    private static final int LONGUEUR_MAX_TITRE = 255;

    private final HalClient halClient;
    private final PersonneRepository personneRepository;
    private final PublicationRepository publicationRepository;

    public ImportHal(HalClient halClient, PersonneRepository personneRepository,
            PublicationRepository publicationRepository) {
        this.halClient = halClient;
        this.personneRepository = personneRepository;
        this.publicationRepository = publicationRepository;
    }

    /** Lève {@link RestClientException} si HAL est injoignable. */
    @Transactional
    public ResultatImportHal importerPourPersonne(Long personneId) {
        Personne personne = personneRepository.findById(personneId).orElseThrow();
        ResultatImportHal resultat = new ResultatImportHal();
        importer(personne, resultat);
        return resultat;
    }

    /** Importe pour chaque personne ayant un idHal ; un échec HAL n'interrompt pas les autres. */
    @Transactional
    public ResultatImportHal importerPourTous() {
        ResultatImportHal resultat = new ResultatImportHal();
        for (Personne personne : personneRepository.findAll()) {
            if (aUnIdHal(personne)) {
                try {
                    importer(personne, resultat);
                } catch (RestClientException e) {
                    resultat.personnesEnEchec.add(personne.getNomComplet());
                }
            }
        }
        return resultat;
    }

    public static boolean aUnIdHal(Personne personne) {
        return personne.getIdHal() != null && !personne.getIdHal().isBlank();
    }

    private void importer(Personne personne, ResultatImportHal resultat) {
        if (!aUnIdHal(personne)) {
            return;
        }
        String idHal = personne.getIdHal().trim();
        for (DocumentHal doc : halClient.publicationsDe(idHal)) {
            if (doc.halId() == null || doc.titre() == null || doc.titre().isBlank()) {
                continue;
            }
            Publication publication = publicationRepository.findFirstByIdHal(doc.halId()).orElse(null);
            if (publication == null) {
                publication = new Publication();
                publication.setIdHal(doc.halId());
                resultat.publicationsCreees.add(doc.halId());
            } else if (!resultat.publicationsCreees.contains(doc.halId())) {
                resultat.publicationsExistantes.add(doc.halId());
            }
            publication.setTitre(tronquer(doc.titre()));
            publication.setAnnee(doc.annee());
            publication = publicationRepository.save(publication);

            Set<String> idHalAuteurs = new HashSet<>(doc.idHalAuteurs());
            idHalAuteurs.add(idHal);
            for (Personne auteur : personneRepository.findByIdHalIn(idHalAuteurs)) {
                if (!auteur.getPublications().contains(publication)) {
                    auteur.getPublications().add(publication);
                    resultat.liensAjoutes++;
                }
            }
        }
        resultat.personnesTraitees++;
    }

    private static String tronquer(String titre) {
        String t = titre.trim();
        return t.length() <= LONGUEUR_MAX_TITRE ? t : t.substring(0, LONGUEUR_MAX_TITRE - 1) + "…";
    }
}
