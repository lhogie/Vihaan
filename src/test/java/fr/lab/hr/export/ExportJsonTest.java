package fr.lab.hr.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.lab.hr.model.Bureau;
import fr.lab.hr.model.Genre;
import fr.lab.hr.model.Personne;
import fr.lab.hr.model.Poste;
import fr.lab.hr.model.Structure;

class ExportJsonTest {

    @Test
    void exporteChampsSimplesEtRelationsProprietairesParIdentifiant() {
        Structure equipe = new Structure();
        equipe.setId(17L);
        Bureau b1 = bureau(3L);
        Bureau b2 = bureau(1L);
        Personne personne = new Personne();
        personne.setId(42L);
        personne.setNom("Martin");
        personne.setPrenom("Alice");
        personne.setGenre(Genre.F);
        personne.setDateNaissance(LocalDate.of(1980, 5, 17));
        personne.setStructure(equipe);
        personne.setBureauxSecondaires(List.of(b1, b2));
        personne.getEmails().add("alice@i3s.fr");
        Poste poste = new Poste();
        poste.setId(9L);
        personne.getPostes().add(poste);

        Map<String, Object> map = ExportJson.versMap(personne);

        assertEquals(42L, map.get("id"));
        assertEquals("Martin", map.get("nom"));
        assertEquals("F", map.get("genre"));
        assertEquals("1980-05-17", map.get("dateNaissance"));
        assertEquals(100, map.get("quotiteTravail"));
        assertEquals(17L, map.get("structureId"));
        assertNull(map.get("bureauPrincipalId"));
        assertEquals(List.of(1L, 3L), map.get("bureauxSecondairesIds"));
        assertEquals(List.of("alice@i3s.fr"), map.get("emails"));
        assertFalse(map.containsKey("postes"), "côté inverse (mappedBy) non exporté");
        assertFalse(map.containsKey("postesIds"), "côté inverse (mappedBy) non exporté");
        assertFalse(map.containsKey("contratsPortesIds"), "côté inverse (mappedBy) non exporté");
    }

    @Test
    void lesClesDeChaqueObjetCommencentParLId() {
        Map<String, Object> map = ExportJson.versMap(bureau(5L));
        assertEquals("id", map.keySet().iterator().next());
        assertEquals(List.of("id", "nom", "capacite", "batimentId"), List.copyOf(map.keySet()));
    }

    private static Bureau bureau(Long id) {
        Bureau bureau = new Bureau();
        bureau.setId(id);
        bureau.setNom("B" + id);
        return bureau;
    }
}
