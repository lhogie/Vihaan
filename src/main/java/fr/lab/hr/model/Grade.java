package fr.lab.hr.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Grade implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String code;

    @NotBlank
    private String libelle;

    @ManyToMany(mappedBy = "grades")
    private List<Employeur> employeurs = new ArrayList<>();

    @OneToMany(mappedBy = "grade")
    private List<Poste> postes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public List<Employeur> getEmployeurs() {
        return employeurs;
    }

    public void setEmployeurs(List<Employeur> employeurs) {
        this.employeurs = employeurs;
    }

    public List<Poste> getPostes() {
        return postes;
    }

    public void setPostes(List<Poste> postes) {
        this.postes = postes;
    }

    @Override
    public String toString() {
        return code + " - " + libelle;
    }
}
