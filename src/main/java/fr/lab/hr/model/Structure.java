package fr.lab.hr.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Structure implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nom;

    /** Texte libre : thématiques, missions... TEXT plutôt que VARCHAR(255). */
    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    private Structure structureParente;

    @OneToMany(mappedBy = "structureParente", cascade = CascadeType.PERSIST)
    private List<Structure> sousStructures = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "structure_bureau")
    private List<Bureau> bureaux = new ArrayList<>();

    @OneToMany(mappedBy = "structure")
    private List<Personne> personnesPrincipal = new ArrayList<>();

    @ManyToMany(mappedBy = "structuresImpliquees")
    private List<Personne> personnesImpliquees = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Structure getStructureParente() {
        return structureParente;
    }

    public void setStructureParente(Structure structureParente) {
        this.structureParente = structureParente;
    }

    public List<Structure> getSousStructures() {
        return sousStructures;
    }

    public void setSousStructures(List<Structure> sousStructures) {
        this.sousStructures = sousStructures;
    }

    public List<Bureau> getBureaux() {
        return bureaux;
    }

    public void setBureaux(List<Bureau> bureaux) {
        this.bureaux = bureaux;
    }

    public List<Personne> getPersonnesPrincipal() {
        return personnesPrincipal;
    }

    public void setPersonnesPrincipal(List<Personne> personnesPrincipal) {
        this.personnesPrincipal = personnesPrincipal;
    }

    public List<Personne> getPersonnesImpliquees() {
        return personnesImpliquees;
    }

    public void setPersonnesImpliquees(List<Personne> personnesImpliquees) {
        this.personnesImpliquees = personnesImpliquees;
    }

    @Override
    public String toString() {
        return nom;
    }
}
