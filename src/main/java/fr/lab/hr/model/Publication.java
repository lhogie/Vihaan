package fr.lab.hr.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Publication implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String titre;

    private Integer annee;

    private String idHal;

    @ManyToMany(mappedBy = "publications")
    private List<Personne> auteurs = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public String getIdHal() {
        return idHal;
    }

    public void setIdHal(String idHal) {
        this.idHal = idHal;
    }

    public List<Personne> getAuteurs() {
        return auteurs;
    }

    public void setAuteurs(List<Personne> auteurs) {
        this.auteurs = auteurs;
    }

    @Override
    public String toString() {
        return titre;
    }
}
