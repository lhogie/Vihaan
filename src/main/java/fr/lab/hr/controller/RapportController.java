package fr.lab.hr.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import fr.lab.hr.rapport.RapportMarkdown;

@Controller
public class RapportController {

    private final RapportMarkdown rapportMarkdown;

    public RapportController(RapportMarkdown rapportMarkdown) {
        this.rapportMarkdown = rapportMarkdown;
    }

    @GetMapping("/rapport")
    public ResponseEntity<String> telecharger() {
        LocalDate aujourdhui = LocalDate.now();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("rapport-" + aujourdhui + ".md").build().toString())
                .body(rapportMarkdown.generer(aujourdhui));
    }
}
