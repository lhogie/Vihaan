package fr.lab.hr.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Contrat implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nom;

    @ManyToOne
    private Personne porteur;

    @ManyToMany
    @JoinTable(name = "contrat_sous_porteur")
    private List<Personne> sousPorteurs = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "contrat_coordinateur")
    private List<Personne> coordinateurs = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "contrat_partenaire")
    private List<Personne> partenaires = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "contrat_autre_personne")
    private List<Personne> autresPersonnes = new ArrayList<>();

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

    public Personne getPorteur() {
        return porteur;
    }

    public void setPorteur(Personne porteur) {
        this.porteur = porteur;
    }

    public List<Personne> getSousPorteurs() {
        return sousPorteurs;
    }

    public void setSousPorteurs(List<Personne> sousPorteurs) {
        this.sousPorteurs = sousPorteurs;
    }

    public List<Personne> getCoordinateurs() {
        return coordinateurs;
    }

    public void setCoordinateurs(List<Personne> coordinateurs) {
        this.coordinateurs = coordinateurs;
    }

    public List<Personne> getPartenaires() {
        return partenaires;
    }

    public void setPartenaires(List<Personne> partenaires) {
        this.partenaires = partenaires;
    }

    public List<Personne> getAutresPersonnes() {
        return autresPersonnes;
    }

    public void setAutresPersonnes(List<Personne> autresPersonnes) {
        this.autresPersonnes = autresPersonnes;
    }

    @Override
    public String toString() {
        return nom;
    }
}
