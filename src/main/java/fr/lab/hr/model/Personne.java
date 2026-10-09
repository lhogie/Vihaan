package fr.lab.hr.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Personne implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    private String prenomUsage;

    private String nomJeuneFille;

    @Enumerated(EnumType.STRING)
    private Genre genre;

    private String numeroBadge;

    private String villeNaissance;

    private LocalDate dateNaissance;

    private LocalDate dateDoctorat;

    private boolean estHDR;

    private boolean estAssocie;

    private boolean estCollaborateur;

    private Integer quotiteTravail;

    private String orcid;

    private String idHal;

    private String sectionCNRS;

    @ElementCollection
    @CollectionTable(name = "personne_telephone")
    private List<String> telephones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "personne_email")
    private List<String> emails = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "personne_page_web")
    private List<String> pagesWeb = new ArrayList<>();

    @ManyToOne
    private Adresse adressePostale;

    @ManyToOne
    private Bureau bureauPrincipal;

    @ManyToMany
    @JoinTable(name = "personne_bureau_secondaire")
    private List<Bureau> bureauxSecondaires = new ArrayList<>();

    @ManyToOne
    private Structure structure;

    @ManyToMany
    @JoinTable(name = "personne_structure_impliquee")
    private List<Structure> structuresImpliquees = new ArrayList<>();

    @OneToMany(mappedBy = "personne")
    private List<Poste> postes = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "personne_publication")
    private List<Publication> publications = new ArrayList<>();

    @OneToMany(mappedBy = "porteur")
    private List<Contrat> contratsPortes = new ArrayList<>();

    @ManyToMany(mappedBy = "sousPorteurs")
    private List<Contrat> contratsSousPortes = new ArrayList<>();

    @ManyToMany(mappedBy = "coordinateurs")
    private List<Contrat> contratsCoordines = new ArrayList<>();

    @ManyToMany(mappedBy = "partenaires")
    private List<Contrat> contratsPartenaire = new ArrayList<>();

    @ManyToMany(mappedBy = "autresPersonnes")
    private List<Contrat> contratsAutre = new ArrayList<>();

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

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getPrenomUsage() {
        return prenomUsage;
    }

    public void setPrenomUsage(String prenomUsage) {
        this.prenomUsage = prenomUsage;
    }

    public String getNomJeuneFille() {
        return nomJeuneFille;
    }

    public void setNomJeuneFille(String nomJeuneFille) {
        this.nomJeuneFille = nomJeuneFille;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public String getNumeroBadge() {
        return numeroBadge;
    }

    public void setNumeroBadge(String numeroBadge) {
        this.numeroBadge = numeroBadge;
    }

    public String getVilleNaissance() {
        return villeNaissance;
    }

    public void setVilleNaissance(String villeNaissance) {
        this.villeNaissance = villeNaissance;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public LocalDate getDateDoctorat() {
        return dateDoctorat;
    }

    public void setDateDoctorat(LocalDate dateDoctorat) {
        this.dateDoctorat = dateDoctorat;
    }

    public boolean isEstHDR() {
        return estHDR;
    }

    public void setEstHDR(boolean estHDR) {
        this.estHDR = estHDR;
    }

    public boolean isEstAssocie() {
        return estAssocie;
    }

    public void setEstAssocie(boolean estAssocie) {
        this.estAssocie = estAssocie;
    }

    public boolean isEstCollaborateur() {
        return estCollaborateur;
    }

    public void setEstCollaborateur(boolean estCollaborateur) {
        this.estCollaborateur = estCollaborateur;
    }

    public Integer getQuotiteTravail() {
        return quotiteTravail;
    }

    public void setQuotiteTravail(Integer quotiteTravail) {
        this.quotiteTravail = quotiteTravail;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getIdHal() {
        return idHal;
    }

    public void setIdHal(String idHal) {
        this.idHal = idHal;
    }

    public String getSectionCNRS() {
        return sectionCNRS;
    }

    public void setSectionCNRS(String sectionCNRS) {
        this.sectionCNRS = sectionCNRS;
    }

    public List<String> getTelephones() {
        return telephones;
    }

    public void setTelephones(List<String> telephones) {
        this.telephones = telephones;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getPagesWeb() {
        return pagesWeb;
    }

    public void setPagesWeb(List<String> pagesWeb) {
        this.pagesWeb = pagesWeb;
    }

    public Adresse getAdressePostale() {
        return adressePostale;
    }

    public void setAdressePostale(Adresse adressePostale) {
        this.adressePostale = adressePostale;
    }

    public Bureau getBureauPrincipal() {
        return bureauPrincipal;
    }

    public void setBureauPrincipal(Bureau bureauPrincipal) {
        this.bureauPrincipal = bureauPrincipal;
    }

    public List<Bureau> getBureauxSecondaires() {
        return bureauxSecondaires;
    }

    public void setBureauxSecondaires(List<Bureau> bureauxSecondaires) {
        this.bureauxSecondaires = bureauxSecondaires;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public List<Structure> getStructuresImpliquees() {
        return structuresImpliquees;
    }

    public void setStructuresImpliquees(List<Structure> structuresImpliquees) {
        this.structuresImpliquees = structuresImpliquees;
    }

    public List<Poste> getPostes() {
        return postes;
    }

    public void setPostes(List<Poste> postes) {
        this.postes = postes;
    }

    public List<Publication> getPublications() {
        return publications;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    public List<Contrat> getContratsPortes() {
        return contratsPortes;
    }

    public void setContratsPortes(List<Contrat> contratsPortes) {
        this.contratsPortes = contratsPortes;
    }

    public List<Contrat> getContratsSousPortes() {
        return contratsSousPortes;
    }

    public void setContratsSousPortes(List<Contrat> contratsSousPortes) {
        this.contratsSousPortes = contratsSousPortes;
    }

    public List<Contrat> getContratsCoordines() {
        return contratsCoordines;
    }

    public void setContratsCoordines(List<Contrat> contratsCoordines) {
        this.contratsCoordines = contratsCoordines;
    }

    public List<Contrat> getContratsPartenaire() {
        return contratsPartenaire;
    }

    public void setContratsPartenaire(List<Contrat> contratsPartenaire) {
        this.contratsPartenaire = contratsPartenaire;
    }

    public List<Contrat> getContratsAutre() {
        return contratsAutre;
    }

    public void setContratsAutre(List<Contrat> contratsAutre) {
        this.contratsAutre = contratsAutre;
    }

    public String getNomComplet() {
        return (prenomUsage != null && !prenomUsage.isBlank() ? prenomUsage : prenom) + " " + nom;
    }

    @Override
    public String toString() {
        return getNomComplet();
    }
}
