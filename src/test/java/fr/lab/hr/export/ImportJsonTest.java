package fr.lab.hr.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.lab.hr.model.Genre;

class ImportJsonTest {

    @Test
    void accepteUnExportCompletMemeAvecDesTypesVides() {
        Map<String, Object> donnees = exportComplet();
        donnees.put("personnes", List.of(Map.of("id", 1, "nom", "Martin")));

        Map<String, List<Map<String, Object>>> lignes = ImportJson.verifier(donnees);

        assertEquals(ExportJson.TYPES.keySet(), lignes.keySet());
        assertEquals(1, lignes.get("personnes").size());
    }

    @Test
    void refuseUnExportDUnSeulTypePourNePasViderLesAutres() {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("personnes", List.of());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ImportJson.verifier(donnees));
        assertTrue(e.getMessage().contains("pas un export complet"), e.getMessage());
        assertTrue(e.getMessage().contains("structures"), e.getMessage());
    }

    @Test
    void refuseUnElementSansIdentifiant() {
        Map<String, Object> donnees = exportComplet();
        donnees.put("grades", List.of(Map.of("code", "DR")));

        assertThrows(IllegalArgumentException.class, () -> ImportJson.verifier(donnees));
    }

    @Test
    void convertitLesValeursJsonVersLeTypeDuChamp() {
        assertEquals(LocalDate.of(2024, 1, 31), ImportJson.convertir("2024-01-31", LocalDate.class));
        assertEquals("F", ImportJson.convertir("F", Genre.class));
        assertEquals(80, ImportJson.convertir(80L, Integer.class));
        assertEquals(43.6, ImportJson.convertir(43.6, Double.class));
        assertEquals(Boolean.TRUE, ImportJson.convertir(true, boolean.class));
        assertEquals(Boolean.FALSE, ImportJson.convertir(null, boolean.class), "booléen primitif absent");
        assertNull(ImportJson.convertir(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> ImportJson.convertir("X", Genre.class));
    }

    private static Map<String, Object> exportComplet() {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("genereLe", "2026-10-09");
        ExportJson.TYPES.keySet().forEach(type -> donnees.put(type, List.of()));
        return donnees;
    }
}
