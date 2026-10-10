package fr.lab.hr.hal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HalClientTest {

    private static final String REPONSE = """
            {"response":{"numFound":2,"docs":[
              {"halId_s":"hal-04878642","title_s":["Titre principal","Sous-titre"],
               "producedDateY_i":2024,"authIdHal_s":["lhogie"],"authIdHal_i":[1334712]},
              {"halId_s":"hal-00000001","title_s":["Sans auteur identifie"]}
            ]}}
            """;

    @Test
    void interrogeHalParIdHalTextuelEtConvertitLesDocuments() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(queryParam("q", "authIdHal_s%3A%22lhogie%22"))
                .andRespond(withSuccess(REPONSE, MediaType.APPLICATION_JSON));

        List<DocumentHal> docs = new HalClient(builder, "https://hal.test/search/").publicationsDe("lhogie");

        serveur.verify();
        assertEquals(new DocumentHal("hal-04878642", "Titre principal", 2024, Set.of("lhogie", "1334712")),
                docs.get(0));
        assertEquals(new DocumentHal("hal-00000001", "Sans auteur identifie", null, Set.of()), docs.get(1));
    }

    @Test
    void utiliseLeChampNumeriquePourUnIdHalNumerique() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur.expect(queryParam("q", "authIdHal_i%3A1334712"))
                .andRespond(withSuccess("{\"response\":{\"docs\":[]}}", MediaType.APPLICATION_JSON));

        assertEquals(List.of(), new HalClient(builder, "https://hal.test/search/").publicationsDe("1334712"));
        serveur.verify();
    }
}
