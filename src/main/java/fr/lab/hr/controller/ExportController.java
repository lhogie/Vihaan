package fr.lab.hr.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import tools.jackson.databind.json.JsonMapper;

import fr.lab.hr.export.ExportJson;

@Controller
public class ExportController {

    /** Horodatage des noms de fichiers ; sans « : », interdit dans les noms de fichiers sous Windows. */
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ExportJson exportJson;
    private final JsonMapper jsonMapper;

    public ExportController(ExportJson exportJson, JsonMapper jsonMapper) {
        this.exportJson = exportJson;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/export/json")
    public ResponseEntity<String> tout() {
        LocalDateTime maintenant = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("genereLe", maintenant.toString());
        export.putAll(exportJson.tout());
        return fichier("export-" + HORODATAGE.format(maintenant) + ".json", export);
    }

    @GetMapping("/export/json/{type}")
    public ResponseEntity<String> parType(@PathVariable String type) {
        return exportJson.parType(type)
                .map(lignes -> fichier(type + "-" + HORODATAGE.format(LocalDateTime.now()) + ".json", lignes))
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<String> fichier(String nom, Object contenu) {
        return ResponseEntity.ok()
                .contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
                .body(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contenu));
    }
}
