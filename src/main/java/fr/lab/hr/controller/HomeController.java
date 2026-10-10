package fr.lab.hr.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String accueil() {
        return "redirect:/personnes";
    }

    /** Formulaire de connexion ; le POST /login est traité par Spring Security. */
    @GetMapping("/login")
    public String connexion() {
        return "login";
    }
}
