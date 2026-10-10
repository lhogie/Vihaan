package fr.lab.hr.hal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import fr.lab.hr.model.Personne;
import fr.lab.hr.model.Publication;
import fr.lab.hr.repository.PersonneRepository;
import fr.lab.hr.repository.PublicationRepository;

class ImportHalTest {

    private HalClient halClient;
    private PersonneRepository personneRepository;
    private PublicationRepository publicationRepository;
    private ImportHal importHal;

    @BeforeEach
    void setUp() {
        halClient = mock(HalClient.class);
        personneRepository = mock(PersonneRepository.class);
        publicationRepository = mock(PublicationRepository.class);
        when(publicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        importHal = new ImportHal(halClient, personneRepository, publicationRepository);
    }

    @Test
    void creeLaPublicationEtLieLAuteurEtLesCoAuteursDuLabo() {
        Personne alice = personne(1L, "alice");
        Personne bob = personne(2L, "bob");
        when(personneRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(halClient.publicationsDe("alice"))
                .thenReturn(List.of(new DocumentHal("hal-1", "Un article", 2024, Set.of("alice", "bob", "externe"))));
        when(publicationRepository.findFirstByIdHal("hal-1")).thenReturn(Optional.empty());
        when(personneRepository.findByIdHalIn(anyCollection())).thenReturn(List.of(alice, bob));

        ResultatImportHal resultat = importHal.importerPourPersonne(1L);

        assertEquals(1, resultat.getPublicationsCreees());
        assertEquals(2, resultat.getLiensAjoutes());
        Publication publication = alice.getPublications().get(0);
        assertEquals("hal-1", publication.getIdHal());
        assertEquals("Un article", publication.getTitre());
        assertEquals(2024, publication.getAnnee());
        assertSame(publication, bob.getPublications().get(0));
    }

    @Test
    void metAJourUnePublicationExistanteSansDoublonNiDoubleLien() {
        Personne alice = personne(1L, "alice");
        Publication existante = new Publication();
        existante.setIdHal("hal-1");
        existante.setTitre("Ancien titre");
        alice.getPublications().add(existante);
        when(personneRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(halClient.publicationsDe("alice"))
                .thenReturn(List.of(new DocumentHal("hal-1", "Nouveau titre", 2025, Set.of("alice"))));
        when(publicationRepository.findFirstByIdHal("hal-1")).thenReturn(Optional.of(existante));
        when(personneRepository.findByIdHalIn(anyCollection())).thenReturn(List.of(alice));

        ResultatImportHal resultat = importHal.importerPourPersonne(1L);

        assertEquals(0, resultat.getPublicationsCreees());
        assertEquals(1, resultat.getPublicationsExistantes());
        assertEquals(0, resultat.getLiensAjoutes());
        assertEquals(1, alice.getPublications().size());
        assertEquals("Nouveau titre", existante.getTitre());
        assertEquals(2025, existante.getAnnee());
    }

    @Test
    void ignoreLesPersonnesSansIdHalEtContinueApresUnEchec() {
        Personne sansIdHal = personne(1L, " ");
        Personne enEchec = personne(2L, "panne");
        Personne ok = personne(3L, "ok");
        when(personneRepository.findAll()).thenReturn(List.of(sansIdHal, enEchec, ok));
        when(halClient.publicationsDe("panne")).thenThrow(new ResourceAccessException("timeout"));
        when(halClient.publicationsDe("ok")).thenReturn(List.of());

        ResultatImportHal resultat = importHal.importerPourTous();

        assertEquals(1, resultat.getPersonnesTraitees());
        assertEquals(List.of("Prenom panne"), resultat.getPersonnesEnEchec());
        verify(halClient, never()).publicationsDe(" ");
    }

    @Test
    void tronqueLesTitresTropLongsPourLaColonne() {
        Personne alice = personne(1L, "alice");
        when(personneRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(halClient.publicationsDe("alice"))
                .thenReturn(List.of(new DocumentHal("hal-1", "x".repeat(400), null, Set.of())));
        when(publicationRepository.findFirstByIdHal("hal-1")).thenReturn(Optional.empty());
        when(personneRepository.findByIdHalIn(anyCollection())).thenReturn(List.of(alice));

        importHal.importerPourPersonne(1L);

        String titre = alice.getPublications().get(0).getTitre();
        assertEquals(255, titre.length());
        assertTrue(titre.endsWith("…"));
    }

    private static Personne personne(Long id, String idHal) {
        Personne personne = new Personne();
        personne.setId(id);
        personne.setNom(idHal);
        personne.setPrenom("Prenom");
        personne.setIdHal(idHal);
        return personne;
    }
}
