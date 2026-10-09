package fr.lab.hr.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Batiment implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nom;

    @ManyToOne
    private Campus campus;

    @OneToMany(mappedBy = "batiment", cascade = CascadeType.PERSIST)
    private List<Bureau> bureaux = new ArrayList<>();

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

    public Campus getCampus() {
        return campus;
    }

    public void setCampus(Campus campus) {
        this.campus = campus;
    }

    public List<Bureau> getBureaux() {
        return bureaux;
    }

    public void setBureaux(List<Bureau> bureaux) {
        this.bureaux = bureaux;
    }

    @Override
    public String toString() {
        return nom;
    }
}
