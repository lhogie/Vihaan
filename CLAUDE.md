# Instructions pour les agents IA travaillant sur ce dépôt

Application de gestion RH de laboratoire : Spring Boot 4.1 (Java 21) + Spring Data JPA + MariaDB + Thymeleaf. Pas de frontend séparé, pas d'API REST. Connexion obligatoire avec deux rôles, LECTEUR et GESTIONNAIRE (Spring Security, comptes locaux en attendant un SSO). Voir `README.md` pour l'architecture complète et les décisions de conception ; ce fichier ne couvre que ce qu'un agent doit savoir avant de modifier le code.

## Convention de nommage

Tout le vocabulaire métier (classes, champs, templates, routes) est en **français**, pour coller au cahier des charges d'origine. Ne pas traduire en anglais, même partiellement — un mélange des deux rend le code illisible (ex: ne pas renommer `Personne` en `Person` ni ajouter un champ `name` à côté de `nom`).

## Qui possède quelle relation

Avant de toucher à une relation JPA, vérifier quel côté est propriétaire (`@JoinTable`/`@JoinColumn`) et quel côté est inverse (`mappedBy`) :

- `Contrat` possède ses 5 relations vers `Personne` (porteur, sousPorteurs, coordinateurs, partenaires, autresPersonnes). `Personne` n'a que des vues inverses en lecture (`contratsPortes`, etc.).
- `Personne` possède `bureauPrincipal`/`bureauxSecondaires`, `structure`/`structuresImpliquees`, `publications`. Les côtés inverses (`Bureau.personnesPrincipal`, `Structure.personnesImpliquees`, `Publication.auteurs`...) sont en lecture seule : **on ne les édite jamais depuis le formulaire de l'entité inverse**, seulement depuis le formulaire de `Personne`.
- `Employeur` possède `grades` (ManyToMany vers `Grade`).
- Les relations ManyToOne (`Batiment.campus`, `Bureau.batiment`, `Adresse.pays`, `Poste.employeur/grade/personne`, `Structure.structureParente`) sont éditées via un `<select>` qui poste un `xxxId` séparé (pas de binding direct Spring sur l'objet relation — voir pattern ci-dessous).

Si une nouvelle relation est ajoutée, elle doit être mappée dans les deux sens (`mappedBy`) : le cahier des charges exige que chaque relation soit navigable dans les deux sens depuis l'UI.

## Piège cascade/orphanRemoval (déjà rencontré une fois)

**Ne jamais mettre `cascade = CascadeType.ALL, orphanRemoval = true`** sur une collection `@OneToMany` dont le formulaire d'édition de l'entité parente ne resoumet pas la collection à chaque sauvegarde. Chaque `@PostMapping` de ce projet reconstruit l'entité via `@ModelAttribute` (donc avec les collections non concernées remises à leur valeur par défaut, souvent une liste vide) — si cette collection est en cascade+orphanRemoval, Hibernate supprimera silencieusement toutes les lignes enfants en base à la sauvegarde suivante du parent.

C'est exactement ce qui s'est produit avec `Personne.postes` (corrigé : plus de cascade, juste une vue inverse en lecture). Avant d'ajouter un cascade sur une relation, vérifier si l'entité enfant a son propre contrôleur CRUD — si oui, pas besoin de cascade côté parent.

## Pattern des contrôleurs CRUD

Les 12 contrôleurs (`src/main/java/fr/lab/hr/controller/`) suivent tous le même schéma : `list` / `new` (GET) / `{id}/edit` (GET) / `show` (`GET /{id}`) / `save` (`POST`, sert à la fois création et modification via le champ caché `id`) / `delete` (`POST /{id}/delete`).

Pour les relations, pas de `Converter<String, Entity>` Spring enregistré globalement — chaque contrôleur reçoit les ids des entités liées en `@RequestParam` séparés (`campusId`, `bureauxIds`, etc.) et résout lui-même via le repository avant d'appeler `setXxx(...)`. C'est volontairement explicite plutôt que magique ; garder ce pattern pour toute nouvelle relation plutôt que d'introduire des converters.

Les listes de chaînes simples (`Personne.telephones/emails/pagesWeb`, `@ElementCollection`) passent par un `<textarea>` (une valeur par ligne) plutôt que des inputs dynamiques en JS, et sont parsées côté contrôleur (`linesToList`). Garder ce pattern pour toute nouvelle liste de ce type.

## Recherche

Ne pas écrire de requête de recherche par entité. `GenericSearch.containing(Class<T>, String)` (`src/main/java/fr/lab/hr/search/GenericSearch.java`) construit par réflexion un `LIKE` sur tous les champs `String` déclarés d'une entité — toute nouvelle entité ayant des champs texte est automatiquement cherchable, il n'y a rien à faire. Si une entité est ajoutée, il faut seulement l'ajouter à la liste dans `SearchController.rechercher()`.

## Lancer / vérifier

```bash
docker compose up -d
mvn spring-boot:run
```

Avant de considérer une modification UI terminée, vérifier au minimum :
1. `mvn compile` passe.
2. La page liste, le formulaire (new + edit) et la page de détail de l'entité touchée répondent 200, une fois connecté avec le compte `gestionnaire` (sinon redirection vers `/login`). Toute nouvelle URL d'écriture doit être couverte par les règles de `securite/ConfigurationSecurite.java` (POST et formulaires réservés au GESTIONNAIRE) et ses boutons masqués par `th:if="${peutModifier}"` ; `SecuriteTest` vérifie ces règles.
3. Si la modification touche une relation : vérifier en base (`mariadb ... -e "SELECT * FROM <table_de_jointure>"`) que la relation est bien écrite, pas seulement que la page s'affiche.

Les tests (`mvn test`) couvrent l'import HAL, le rapport Markdown, l'export et la restauration JSON, le vidage et les droits d'accès (`SecuriteTest`). Ils utilisent la base de développement en lecture seule : aucun test ne doit la modifier. Si une logique non triviale est ajoutée (ex: règle de validation métier), ajouter un test plutôt que de vérifier seulement manuellement.
