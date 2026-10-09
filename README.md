# Gestion RH du laboratoire

Application de gestion des ressources humaines d'un laboratoire de recherche : personnes, structures, contrats, employeurs, postes, locaux (bureaux/bâtiments/campus), adresses, pays et publications.

## Stack technique

- **Spring Boot 4.1** (Java 21), **Spring Data JPA** (Hibernate), **MariaDB**
- **Thymeleaf** pour une interface web rendue côté serveur (pas de SPA séparée)
- **Maven** comme outil de build
- Pas d'authentification en v1

## Modèle de données

12 entités JPA dans `src/main/java/fr/lab/hr/model/` :

| Entité | Description |
|---|---|
| `Personne` | identité, statut (HDR, associé, collaborateur...), coordonnées, liens vers bureau/structure/postes/publications/contrats |
| `Structure` | hiérarchie de sous-structures, bureaux associés |
| `Contrat` | porteur, sous-porteurs, coordinateurs, partenaires, autres personnes (5 relations distinctes vers `Personne`) |
| `Employeur` | nom, public ou non, catalogue de `Grade` proposés |
| `Grade` | catalogue de référence (IE, IR, AI, DR, CR, MCF, PR), pré-rempli au démarrage |
| `Poste` | lien personne ↔ employeur ↔ grade, avec dates de début/fin |
| `Bureau` | capacité, bâtiment, occupants (principal/secondaire) |
| `Batiment` | ensemble de bureaux, rattaché à un campus |
| `Campus` | ensemble de bâtiments |
| `Adresse` | rue/ville/code postal/GPS/pays |
| `Pays` | table de référence ISO, pré-remplie au démarrage (249 pays) |
| `Publication` | titre/année/identifiant HAL, auteurs (`Personne`) |

Toutes les relations sont mappées dans les deux sens (`mappedBy`) : chaque page de détail affiche et lie les entités associées, dans les deux directions, comme demandé dans le cahier des charges.

### Décisions de modélisation notables

- **Bureaux d'une personne** : un bureau principal (`ManyToOne`) + une liste de bureaux secondaires (`ManyToMany`), le cahier des charges mentionnant les deux sans les distinguer.
- **Identité** : ajout de `nom`/`prenom` standards en plus de `nomJeuneFille`/`prenomUsage`, qui n'étaient pas prévus comme champs d'identité principaux dans le texte d'origine.
- **Employeur/Grade** : plutôt que de sous-classer EPST/Université en dur dans le code, chaque `Employeur` choisit librement ses `Grade` dans un catalogue commun (table `employeur_grade`). Ça traduit "les employeurs publics définissent des statuts" sans figer la règle en Java — modifiable depuis l'UI.
- **Publications** : entité minimale (titre, année, identifiant HAL). Les auteurs se gèrent depuis la fiche de la personne (côté propriétaire de la relation), pas depuis la fiche de la publication.

## Recherche globale

Pas de requête de recherche écrite à la main par entité. `GenericSearch` (`src/main/java/fr/lab/hr/search/GenericSearch.java`) construit par réflexion une `Specification` JPA qui fait un `LIKE` sur tous les champs `String` déclarés d'une entité. `SearchController` l'applique aux 12 repositories et agrège les résultats par type sur `/recherche?q=...`. La barre de recherche est dans la navbar, sur toutes les pages.

## Structure du projet

```
src/main/java/fr/lab/hr/
  model/        12 entités JPA + enum Genre + interface Identifiable
  repository/   12 interfaces Spring Data JPA (JpaRepository + JpaSpecificationExecutor)
  controller/   12 contrôleurs CRUD (list/new/edit/show/save/delete) + SearchController + HomeController
  search/       GenericSearch, SearchHit, SearchResultGroup
  config/       DataSeeder (peuple Pays et Grade au premier démarrage)
src/main/resources/
  templates/    3 pages Thymeleaf par entité (list/form/show) + layout/fragments.html (navbar commune) + search/results.html
  application.yml
docker-compose.yml   MariaDB locale pour le développement
```

Chaque entité suit le même schéma : un formulaire avec des `<select>` (simples ou multiples) pour les relations vers des entités existantes, une page de liste avec ajout/suppression, une page de détail qui liste les entités liées avec des liens cliquables dans les deux sens.

## Lancer le projet

```bash
docker compose up -d        # démarre une MariaDB locale (base "hr", utilisateur "hr"/"hr")
mvn spring-boot:run          # démarre l'application sur http://localhost:8080
```

Au premier démarrage, `DataSeeder` remplit automatiquement les tables `pays` (liste ISO complète, via `Locale.getISOCountries()`) et `grade` (IE/IR/AI/DR/CR/MCF/PR). Ne fait rien aux démarrages suivants si les tables ne sont pas vides.

## Vérifications effectuées

L'application a été testée de bout en bout (MariaDB isolée temporaire, car Docker Desktop n'était pas lancé sur la machine) :

- Les 12 pages de liste, les 12 formulaires de création et plusieurs pages de détail répondent 200.
- Création en chaîne : Campus → Bâtiment → Bureau → Structure → Employeur (avec grades) → Personne → Poste/Contrat/Publication, avec vérification en base que les tables de jointure many-to-many sont bien peuplées.
- Recherche globale testée avec un terme présent sur une personne : résultat correctement groupé avec lien cliquable.
- Suppression testée (poste), la page de la personne restant fonctionnelle après.

**Bug trouvé et corrigé en cours de route** : `Personne.postes` était mappé avec `cascade = CascadeType.ALL, orphanRemoval = true`. Comme le formulaire d'édition d'une personne ne resoumet pas la liste de ses postes (gérés depuis leur propre page), chaque enregistrement d'une personne supprimait silencieusement tous ses postes en base. Corrigé en retirant le cascade/orphanRemoval : `postes` est maintenant une simple vue inverse en lecture (comme les autres collections inverses de l'application).

## Limites connues / pistes pour la suite

- Pas d'authentification (voulu pour la v1).
- `ddl-auto: update` (pas de migrations Flyway) — à revoir si l'app évolue vers de la production.
- Les grades disponibles par employeur ne sont pas appliqués comme contrainte stricte sur le formulaire `Poste` (tous les grades sont proposés) — cohérent avec l'objectif "minimaliste" du cahier des charges, mais pourrait être filtré en JS si besoin.
- La recherche globale ne porte que sur les champs texte directs de chaque entité, pas sur les champs des entités liées (ex. chercher une personne par le nom de sa structure ne la fera pas remonter directement, mais la structure elle-même apparaîtra dans les résultats).
