package fr.lab.hr.securite;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Droits d'accès de bout en bout (filtres Spring Security compris). Les cas
 * d'écriture vérifient seulement que la requête est refusée : aucun test ne
 * modifie la base.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecuriteTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sansConnexionToutRenvoieVersLaPageDeConnexion() throws Exception {
        mvc.perform(get("/personnes")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/css/theme.css")).andExpect(status().isOk());
    }

    @Test
    void lesComptesDeDeveloppementPermettentDeSeConnecter() throws Exception {
        mvc.perform(formLogin().user("lecteur").password("lecteur")).andExpect(authenticated().withRoles("LECTEUR"));
        mvc.perform(formLogin().user("gestionnaire").password("gestionnaire"))
                .andExpect(authenticated().withRoles("GESTIONNAIRE"));
        mvc.perform(formLogin().user("lecteur").password("faux")).andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithMockUser(roles = "LECTEUR")
    void unLecteurConsulteMaisNeModifiePas() throws Exception {
        mvc.perform(get("/personnes")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Ajouter"))))
                .andExpect(content().string(not(containsString("Supprimer"))))
                .andExpect(content().string(not(containsString("Vider la base"))));
        mvc.perform(get("/recherche").param("q", "x")).andExpect(status().isOk());
        mvc.perform(get("/rapport")).andExpect(status().isOk());

        for (String url : new String[] {"/personnes/new", "/personnes/1/edit", "/export/json",
                "/export/json/personnes", "/import", "/vider"}) {
            mvc.perform(get(url)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/vider").param("confirmation", "VIDER").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/personnes/1/delete").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/publications/hal").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GESTIONNAIRE")
    void unGestionnaireALesDroitsDuLecteurEtCeuxDeModification() throws Exception {
        mvc.perform(get("/personnes")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ajouter")))
                .andExpect(content().string(containsString("Vider la base")));
        for (String url : new String[] {"/personnes/new", "/import", "/vider"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
        // Mauvaise confirmation : le contrôleur refuse sans rien modifier.
        mvc.perform(post("/vider").param("confirmation", "non").with(csrf()))
                .andExpect(redirectedUrl("/vider"));
    }

    @Test
    @WithMockUser(roles = "GESTIONNAIRE")
    void uneRequeteSansJetonCsrfEstRefusee() throws Exception {
        mvc.perform(post("/vider").param("confirmation", "VIDER")).andExpect(status().isForbidden());
        mvc.perform(post("/personnes/1/delete")).andExpect(status().isForbidden());
    }
}
