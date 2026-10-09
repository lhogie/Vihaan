package fr.lab.hr.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Bureau implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nom;

    private Integer capacite;

    @ManyToOne
    private Batiment batiment;

    @ManyToMany(mappedBy = "bureaux")
    private List<Structure> structures = new ArrayList<>();

    @OneToMany(mappedBy = "bureauPrincipal")
    private List<Personne> personnesPrincipal = new ArrayList<>();

    @ManyToMany(mappedBy = "bureauxSecondaires")
    private List<Personne> personnesSecondaire = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getCapacite() {
        return capacite;
    }

    public void setCapacite(Integer capacite) {
        this.capacite = capacite;
    }

    public Batiment getBatiment() {
        return batiment;
    }

    public void setBatiment(Batiment batiment) {
        this.batiment = batiment;
    }

    public List<Structure> getStructures() {
        return structures;
    }

    public void setStructures(List<Structure> structures) {
        this.structures = structures;
    }

    public List<Personne> getPersonnesPrincipal() {
        return personnesPrincipal;
    }

    public void setPersonnesPrincipal(List<Personne> personnesPrincipal) {
        this.personnesPrincipal = personnesPrincipal;
    }

    public List<Personne> getPersonnesSecondaire() {
        return personnesSecondaire;
    }

    public void setPersonnesSecondaire(List<Personne> personnesSecondaire) {
        this.personnesSecondaire = personnesSecondaire;
    }

    @Override
    public String toString() {
        return nom;
    }
}
