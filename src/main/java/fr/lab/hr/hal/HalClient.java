package fr.lab.hr.hal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client minimal de l'API de recherche HAL (https://api.archives-ouvertes.fr/docs/search).
 * Renvoie toutes les publications d'un auteur identifié par son idHal, sous
 * forme textuelle (ex. "lhogie") ou numérique (ex. "1334712").
 */
@Component
public class HalClient {

    static final String URL_PAR_DEFAUT = "https://api.archives-ouvertes.fr/search/";
    private static final String CHAMPS = "halId_s,title_s,producedDateY_i,authIdHal_s,authIdHal_i";
    private static final int MAX_RESULTATS = 10000;

    private final RestClient restClient;

    @Autowired
    public HalClient(@Value("${hal.api-url:" + URL_PAR_DEFAUT + "}") String apiUrl) {
        this(RestClient.builder(), apiUrl);
    }

    HalClient(RestClient.Builder builder, String apiUrl) {
        this.restClient = builder.baseUrl(apiUrl).build();
    }

    public List<DocumentHal> publicationsDe(String idHal) {
        String requete = idHal.chars().allMatch(Character::isDigit)
                ? "authIdHal_i:" + idHal
                : "authIdHal_s:\"" + idHal + "\"";
        ReponseHal reponse = restClient.get()
                .uri(b -> b.queryParam("q", "{q}")
                        .queryParam("fl", CHAMPS)
                        .queryParam("rows", MAX_RESULTATS)
                        .queryParam("wt", "json")
                        .build(Map.of("q", requete)))
                .retrieve()
                .body(ReponseHal.class);
        if (reponse == null || reponse.response() == null || reponse.response().docs() == null) {
            return List.of();
        }
        return reponse.response().docs().stream().map(DocBrut::versDocument).toList();
    }

    record ReponseHal(Corps response) {
    }

    record Corps(List<DocBrut> docs) {
    }

    record DocBrut(
            @JsonProperty("halId_s") String halId,
            @JsonProperty("title_s") List<String> titres,
            @JsonProperty("producedDateY_i") Integer annee,
            @JsonProperty("authIdHal_s") List<String> idHalAuteurs,
            @JsonProperty("authIdHal_i") List<Long> idHalNumeriquesAuteurs) {

        DocumentHal versDocument() {
            Set<String> auteurs = new LinkedHashSet<>();
            if (idHalAuteurs != null) {
                auteurs.addAll(idHalAuteurs);
            }
            if (idHalNumeriquesAuteurs != null) {
                idHalNumeriquesAuteurs.forEach(id -> auteurs.add(String.valueOf(id)));
            }
            String titre = titres != null && !titres.isEmpty() ? titres.get(0) : null;
            return new DocumentHal(halId, titre, annee, auteurs);
        }
    }
}
