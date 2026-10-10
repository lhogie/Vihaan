package fr.lab.hr.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import tools.jackson.databind.json.JsonMapper;

import fr.lab.hr.config.DataSeeder;
import fr.lab.hr.export.ImportJson;
import fr.lab.hr.repository.GradeRepository;
import fr.lab.hr.repository.PaysRepository;

class ImportControllerTest {

    private ImportJson importJson;
    private DataSeeder dataSeeder;
    private ImportController controller;

    @BeforeEach
    void setUp() {
        importJson = mock(ImportJson.class);
        dataSeeder = mock(DataSeeder.class);
        controller = new ImportController(importJson, JsonMapper.builder().build(), dataSeeder,
                mock(PaysRepository.class), mock(GradeRepository.class));
    }

    @Test
    void neVidePasSansLaConfirmationExacte() {
        for (String saisie : new String[] {null, "", "vider", "oui"}) {
            assertEquals("redirect:/vider", controller.vider(saisie, new RedirectAttributesModelMap()));
        }
        verify(importJson, never()).vider();
        verify(dataSeeder, never()).peupler();
    }

    @Test
    void videPuisRepeupleLesTablesDeReference() {
        assertEquals("redirect:/personnes", controller.vider(" VIDER ", new RedirectAttributesModelMap()));

        InOrder ordre = inOrder(importJson, dataSeeder);
        ordre.verify(importJson).vider();
        ordre.verify(dataSeeder).peupler();
    }
}
