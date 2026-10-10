package fr.lab.hr.config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import fr.lab.hr.model.Grade;
import fr.lab.hr.model.Pays;
import fr.lab.hr.repository.GradeRepository;
import fr.lab.hr.repository.PaysRepository;

/**
 * Pré-remplit les tables de référence (pays, grades) au premier démarrage.
 * Ne fait rien si les tables contiennent déjà des données, pour rester
 * robuste aux redémarrages.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final PaysRepository paysRepository;
    private final GradeRepository gradeRepository;

    public DataSeeder(PaysRepository paysRepository, GradeRepository gradeRepository) {
        this.paysRepository = paysRepository;
        this.gradeRepository = gradeRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        peupler();
    }

    /** Remplit les tables de référence vides ; appelé au démarrage et après « Vider la base ». */
    public void peupler() {
        seedPays();
        seedGrades();
    }

    private void seedPays() {
        if (paysRepository.count() > 0) {
            return;
        }
        for (String code : Locale.getISOCountries()) {
            Pays pays = new Pays();
            pays.setCode(code);
            pays.setNom(Locale.of("", code).getDisplayCountry(Locale.FRENCH));
            paysRepository.save(pays);
        }
    }

    private void seedGrades() {
        if (gradeRepository.count() > 0) {
            return;
        }
        List<Grade> grades = List.of(
                grade("IE", "Ingénieur d'études"),
                grade("IR", "Ingénieur de recherche"),
                grade("AI", "Assistant ingénieur"),
                grade("DR", "Directeur de recherche"),
                grade("CR", "Chargé de recherche"),
                grade("MCF", "Maître de conférences"),
                grade("PR", "Professeur des universités"));
        gradeRepository.saveAll(grades);
    }

    private Grade grade(String code, String libelle) {
        Grade grade = new Grade();
        grade.setCode(code);
        grade.setLibelle(libelle);
        return grade;
    }
}
