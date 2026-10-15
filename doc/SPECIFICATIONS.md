# Spécifications — Gestion RH du laboratoire

Ce document rassemble les exigences fonctionnelles et techniques de l'application, telles qu'elles sont
implémentées. Chaque exigence porte un identifiant (`DON-03`, `HAL-02`…) pour pouvoir y faire référence.
L'architecture (couches, choix techniques, pièges connus) est décrite dans `doc/latex/architecture.pdf`
et dans `README.md` ; ce document décrit **ce que fait** l'application.

## Sommaire

1. [Périmètre](#1-périmètre)
2. [Modèle de données](#2-modèle-de-données)
3. [Écrans de gestion (CRUD)](#3-écrans-de-gestion-crud)
4. [Recherche globale](#4-recherche-globale)
5. [Import des publications depuis HAL](#5-import-des-publications-depuis-hal)
6. [Rapport Markdown](#6-rapport-markdown)
7. [Export JSON](#7-export-json)
8. [Restauration depuis un export JSON](#8-restauration-depuis-un-export-json)
9. [Vider la base](#9-vider-la-base)
10. [Import initial des CSV de l'I3S](#10-import-initial-des-csv-de-li3s)
11. [Interface graphique](#11-interface-graphique)
12. [Connexion et rôles](#12-connexion-et-rôles)
13. [Exigences techniques](#13-exigences-techniques)
14. [Limites connues](#14-limites-connues)

---

## 1. Périmètre

| Id | Exigence |
|---|---|
| PER-01 | L'application gère les ressources humaines d'un laboratoire de recherche : personnes, structures, contrats, employeurs, grades, postes, locaux (campus, bâtiments, bureaux), adresses, pays et publications. |
| PER-02 | C'est une application web unique, rendue côté serveur. Il n'y a ni frontend séparé ni API REST (hors téléchargements d'exports). |
| PER-03 | L'accès demande une connexion. Les droits dépendent du rôle de l'utilisateur (section 12). |
| PER-04 | Tout le vocabulaire métier (écrans, champs, routes, code) est en français. |

## 2. Modèle de données

### 2.1 Entités

Les champs marqués **\*** sont obligatoires.

| Entité | Champs |
|---|---|
| **Personne** | nom\*, prénom\*, prénom d'usage, nom de jeune fille, genre (M, F, AUTRE), numéro de badge, ville et date de naissance, date de doctorat, HDR (oui/non), associé (oui/non), collaborateur (oui/non), quotité de travail (%), section CNRS, ORCID, identifiant HAL, listes de téléphones, d'e-mails et de pages web |
| **Structure** | nom\*, description (texte libre, plusieurs lignes), structure parente (hiérarchie libre : laboratoire › pôle › équipe…) |
| **Contrat** | nom\* |
| **Employeur** | nom\*, public (oui/non), grades proposés |
| **Grade** | code\*, libellé\* |
| **Poste** | date de début, date de fin |
| **Bureau** | nom\*, capacité |
| **Bâtiment** | nom\* |
| **Campus** | nom\* |
| **Adresse** | rue, ville, code postal, latitude, longitude |
| **Pays** | code ISO\*, nom\* |
| **Publication** | titre\*, année, identifiant HAL |

### 2.2 Relations

| Id | Relation | Cardinalité | Édition depuis |
|---|---|---|---|
| REL-01 | Personne → adresse postale | N:1 | Personne |
| REL-02 | Personne → bureau principal | N:1 | Personne |
| REL-03 | Personne → bureaux secondaires | N:M | Personne |
| REL-04 | Personne → structure principale | N:1 | Personne |
| REL-05 | Personne → structures impliquées | N:M | Personne |
| REL-06 | Personne → publications (auteurs) | N:M | Personne |
| REL-07 | Contrat → porteur | N:1 | Contrat |
| REL-08 | Contrat → sous-porteurs, coordinateurs, partenaires, autres personnes | 4 × N:M | Contrat |
| REL-09 | Poste → personne, employeur, grade | 3 × N:1 | Poste |
| REL-10 | Employeur → grades proposés | N:M | Employeur |
| REL-11 | Structure → structure parente | N:1 | Structure |
| REL-12 | Structure → bureaux | N:M | Structure |
| REL-13 | Bureau → bâtiment | N:1 | Bureau |
| REL-14 | Bâtiment → campus | N:1 | Bâtiment |
| REL-15 | Adresse → pays | N:1 | Adresse |

| Id | Exigence |
|---|---|
| DON-01 | Toute relation est **navigable dans les deux sens** : la fiche de chaque entité liste les entités liées, avec des liens, quel que soit le côté qui porte la relation. |
| DON-02 | Une relation ne s'édite que depuis le formulaire de l'entité qui la porte (colonne « Édition depuis »). Sur l'autre fiche, elle est affichée en lecture seule. |
| DON-03 | Les grades ne sont pas figés par type d'employeur : chaque employeur choisit ses grades dans un catalogue commun, modifiable depuis l'interface. |
| DON-04 | Le grade d'une personne est porté par ses **postes** (personne + employeur + grade + dates). Une personne peut avoir plusieurs postes, successifs ou simultanés. |
| DON-05 | Un poste est **en cours** à une date donnée s'il a commencé (ou n'a pas de date de début) et n'est pas terminé (ou n'a pas de date de fin). |
| DON-06 | La quotité de travail d'une nouvelle personne vaut **100 %** par défaut ; elle reste modifiable. |
| DON-07 | L'affichage d'une personne utilise son prénom d'usage s'il est renseigné, sinon son prénom, suivi du nom. |

### 2.3 Données de référence

| Id | Exigence |
|---|---|
| REF-01 | Au démarrage, si la table des pays est vide, elle est remplie avec la liste ISO complète (249 pays, noms en français). |
| REF-02 | Au démarrage, si la table des grades est vide, elle est remplie avec IE, IR, AI, DR, CR, MCF et PR. |
| REF-03 | Ces remplissages ne font rien si les tables contiennent déjà des données. Ils sont aussi exécutés juste après « Vider la base » (VID-04). |

## 3. Écrans de gestion (CRUD)

| Id | Exigence |
|---|---|
| CRUD-01 | Chacune des 12 entités dispose de : une **liste** (`/<type>`), un formulaire de **création** (`/<type>/new`), un formulaire de **modification** (`/<type>/{id}/edit`), une **fiche** (`/<type>/{id}`) et une **suppression** (`POST /<type>/{id}/delete`). |
| CRUD-02 | Les préfixes d'URL sont : `personnes`, `structures`, `contrats`, `employeurs`, `grades`, `postes`, `bureaux`, `batiments`, `campus`, `adresses`, `pays`, `publications`. |
| CRUD-03 | Création et modification utilisent le même formulaire ; un champ caché `id` distingue les deux cas. Après enregistrement, l'utilisateur revient à la liste. |
| CRUD-04 | Les champs obligatoires sont vérifiés : en cas d'erreur, le formulaire est réaffiché avec les valeurs saisies. |
| CRUD-05 | Les relations vers des entités existantes se choisissent dans des listes déroulantes (simples ou multiples). |
| CRUD-06 | Les listes de valeurs simples (téléphones, e-mails, pages web) se saisissent dans une zone de texte, **une valeur par ligne** ; les lignes vides sont ignorées. |
| CRUD-07 | La suppression demande une confirmation dans le navigateur. |
| CRUD-08 | La page d'accueil (`/`) redirige vers la liste des personnes. |

## 4. Recherche globale

| Id | Exigence |
|---|---|
| RECH-01 | Une zone de recherche, présente sur toutes les pages, interroge toutes les entités à la fois (`/recherche?q=…`). |
| RECH-02 | Une entité correspond si l'un de ses champs texte **contient** le terme, sans tenir compte de la casse. |
| RECH-03 | Les résultats sont regroupés par type d'entité, chaque résultat menant à sa fiche ; les types sans résultat ne sont pas affichés. |
| RECH-04 | Tout champ texte d'une entité, y compris ajoutée plus tard, est cherchable sans développement spécifique. |

## 5. Import des publications depuis HAL

| Id | Exigence |
|---|---|
| HAL-01 | La fiche d'une personne propose un bouton **« Mettre à jour depuis HAL »** qui importe toutes ses publications HAL à partir de son identifiant HAL. Le bouton est désactivé, avec une explication, si l'identifiant HAL n'est pas renseigné. |
| HAL-02 | La liste des publications propose le même bouton, qui fait l'import pour **toutes les personnes** ayant un identifiant HAL. |
| HAL-03 | L'identifiant HAL peut être textuel (`lhogie`) ou numérique (`1334712`). |
| HAL-04 | Une publication est identifiée par son identifiant HAL (`hal-xxxxxxx`) : une publication déjà présente est **mise à jour** (titre, année), jamais dupliquée. |
| HAL-05 | Chaque membre du laboratoire dont l'identifiant HAL figure parmi les auteurs HAL d'une publication est lié à cette publication (co-auteurs compris). Un lien existant n'est jamais dupliqué. |
| HAL-06 | Les titres de plus de 255 caractères sont tronqués et terminés par « … ». |
| HAL-07 | Un bilan s'affiche après l'import : personnes traitées, publications créées, publications déjà présentes mises à jour, liens auteur ajoutés. |
| HAL-08 | Si HAL est injoignable, un message d'erreur s'affiche. Lors d'un import pour toutes les personnes, l'échec pour une personne n'interrompt pas les autres ; le bilan nomme les personnes en échec. |
| HAL-09 | L'import est **additif** : une publication retirée de HAL, ou un auteur retiré d'une publication, reste dans l'application. |

## 6. Rapport Markdown

| Id | Exigence |
|---|---|
| RAP-01 | Un bouton **« Rapport Markdown »** (barre latérale) télécharge un rapport de l'état de la base, nommé `rapport-AAAA-MM-JJ.md`. |
| RAP-02 | **Synthèse** : nombre d'enregistrements de chacune des 12 entités. |
| RAP-03 | **Structures** : arbre des structures, avec pour chacune l'effectif direct et, si elle a des sous-structures, l'effectif total en les incluant. |
| RAP-04 | **Postes en cours par grade** et **par employeur**, au sens de DON-05 ; les postes terminés ne sont pas comptés. |
| RAP-05 | **Bureaux** : par bâtiment, nombre de bureaux, de bureaux occupés et d'occupants (bureau principal) ; liste des bureaux dont l'occupation dépasse la capacité. |
| RAP-06 | **Qualité des données** : personnes sans structure, sans e-mail, sans aucun poste, dont tous les postes sont terminés ; postes sans grade, sans employeur ; publications sans auteur du laboratoire. Les personnes concernées sont nommées quand elles sont 20 au plus. |
| RAP-07 | **Annuaire** : par structure (chemin complet, ex. « I3S › SPARKS › WIMMICS »), tableau nom, prénom, grade et employeur du poste en cours le plus récent, bureau, e-mail ; « — » si l'information manque. |
| RAP-08 | Les caractères qui casseraient la mise en forme Markdown (`|`, `*`, `_`, retours à la ligne) sont neutralisés. |

## 7. Export JSON

| Id | Exigence |
|---|---|
| EXP-01 | Un bouton **« Export JSON »** (barre latérale) télécharge **toute la base** (`/export/json`). |
| EXP-02 | Chaque liste propose un bouton **« Export JSON »** qui télécharge les enregistrements de **ce type** seulement (`/export/json/<type>`, avec les préfixes de CRUD-02). Un type inconnu renvoie une erreur 404. |
| EXP-03 | Les fichiers sont nommés avec la date et l'heure de génération : `export-AAAA-MM-JJ_HH-MM-SS.json` pour la base complète, `<type>-AAAA-MM-JJ_HH-MM-SS.json` pour un type. |
| EXP-04 | L'export complet est un objet contenant `genereLe` (date et heure ISO, ex. `2026-10-10T00:03:08`) puis une liste par type d'entité. L'export d'un type est une liste. |
| EXP-05 | Chaque enregistrement contient son `id` en premier, puis ses champs : valeurs simples telles quelles, dates au format `AAAA-MM-JJ`, énumérations par leur nom, listes de valeurs simples sous forme de tableaux. |
| EXP-06 | Les relations sont exportées **par identifiant**, sous le nom utilisé par les formulaires : `xxxId` pour une relation simple (`structureId`), `xxxIds` pour une relation multiple (`bureauxSecondairesIds`), identifiants triés. |
| EXP-07 | Chaque relation n'est exportée qu'une fois, du côté qui la porte (tableau 2.2) : par exemple les postes d'une personne figurent dans `postes[].personneId`, pas dans la personne. |
| EXP-08 | Le JSON est indenté pour rester lisible. Les enregistrements sont triés par identifiant. |
| EXP-09 | Une entité ajoutée au modèle et déclarée dans la liste des types exportés est exportée sans autre développement. |

## 8. Restauration depuis un export JSON

| Id | Exigence |
|---|---|
| RES-01 | Un bouton **« Restaurer »** (barre latérale) ouvre une page permettant d'envoyer un export complet (EXP-01) pour remplacer la base : l'export JSON sert de **sauvegarde**. |
| RES-02 | La restauration **remplace tout le contenu** de la base par celui du fichier. Les données absentes du fichier sont perdues. |
| RES-03 | Les **identifiants du fichier sont conservés** : liens et adresses des fiches redeviennent ceux de la sauvegarde. Les enregistrements créés ensuite reçoivent des identifiants supérieurs. |
| RES-04 | Restaurer un export puis exporter à nouveau produit un fichier **identique** (hors `genereLe`). |
| RES-05 | La restauration est **atomique** : en cas d'erreur, la base n'est pas modifiée du tout. |
| RES-06 | Sont refusés, sans modifier la base, avec un message explicite : un fichier qui n'est pas du JSON ; l'export d'un seul type (il viderait les autres types) ; un export auquel il manque un type ; un enregistrement sans identifiant ; une valeur invalide (ex. genre inconnu) ; une référence vers un identifiant absent du fichier. |
| RES-07 | La page rappelle les conséquences, propose de télécharger d'abord une sauvegarde de la base actuelle, et le navigateur demande une confirmation avant l'envoi. |
| RES-08 | Les fichiers jusqu'à 50 Mo sont acceptés. |
| RES-09 | Après une restauration réussie, l'utilisateur arrive sur la liste des personnes avec le nombre d'enregistrements restaurés par type. |

## 9. Vider la base

| Id | Exigence |
|---|---|
| VID-01 | Un bouton **« Vider la base »** (barre latérale, en rouge) ouvre une page de confirmation ; il ne supprime rien par lui-même. |
| VID-02 | La page explique ce qui sera supprimé et propose de télécharger d'abord une sauvegarde. |
| VID-03 | La suppression n'a lieu que si l'utilisateur **saisit `VIDER`** (vérifié par le serveur, espaces autour tolérés) **et** confirme dans le navigateur. Toute autre saisie est refusée sans rien modifier. |
| VID-04 | Toutes les données sont supprimées, puis les tables de référence sont remplies à nouveau (REF-01, REF-02), comme lors d'une première installation. |
| VID-05 | La suppression est atomique, comme une restauration (RES-05). |
| VID-06 | Un message indique le résultat (nombre de pays et de grades recréés). |

## 10. Import initial des CSV de l'I3S

Import ponctuel, en ligne de commande, des exports CSV du tableau de bord RH de l'I3S (dossier `tbrh`).

| Id | Exigence |
|---|---|
| CSV-01 | `python3 scripts/import_tbrh.py <dossier>` importe les 9 fichiers : `personneI3S_{ATER,C,CDD,DOC,EC,IT,POSTDOC}.csv`, `stagiaire.csv`, `visiteur.csv`. L'option `--dry-run` affiche le SQL sans rien écrire. |
| CSV-02 | L'import est refusé si la table des personnes n'est pas vide, pour éviter les doublons. Il s'exécute en une seule transaction. |
| CSV-03 | Une personne est identifiée par nom, prénom et date de naissance ; si elle apparaît dans plusieurs fichiers, elle reçoit un poste par ligne. |
| CSV-04 | Structures : chaque pôle est rattaché à la structure I3S, chaque équipe à son pôle ; la structure principale d'une personne est son équipe, à défaut son pôle. Les structures existantes de même nom (sans tenir compte de la casse) sont réutilisées. |
| CSV-05 | Postes : employeur = organisme d'appartenance ou gestionnaire (UNS et UniCA regroupés sous UCA ; UCA pour les enseignants-chercheurs) ; grade = corps et grade du fichier (DR1, MCN, PR2…), ou à défaut la catégorie du fichier (DOC, POSTDOC, ATER, CDD, STAGIAIRE, VISITEUR) ; dates d'arrivée et de départ. |
| CSV-06 | Bureaux : rattachés au bâtiment correspondant au site de recherche (ALGORITHMES, TEMPLIERS, INRIA). |
| CSV-07 | Les valeurs de remplissage sont ignorées : dates `0000-00-00`, `9999-12-31`, `2099-12-31` ou invalides, téléphones `(33) 0`, pages web `http://`. Les dates de doctorat partielles (`1998`, `10/2000`) sont ramenées au premier jour de la période. |
| CSV-08 | Les dix colonnes décalées par rapport à leur en-tête dans `personneI3S_EC.csv` sont remises en correspondance. |
| CSV-09 | Ne sont pas importés : adresses privées (illisibles dans les fichiers), téléphones privés et fax, nationalité, pays de naissance, thèse, financements, encadrants, notes libres. |

## 11. Interface graphique

| Id | Exigence |
|---|---|
| IHM-01 | Une barre latérale, présente sur toutes les pages, regroupe la navigation en trois rubriques : **Personnes** (personnes, postes, publications), **Organisation** (structures, contrats, employeurs, grades), **Lieux** (campus, bâtiments, bureaux, adresses, pays). |
| IHM-02 | La rubrique de la page courante est mise en évidence dans la barre latérale. |
| IHM-03 | La barre latérale contient aussi la zone de recherche, une section **Données** (Rapport Markdown ; pour un gestionnaire, aussi Export JSON, Restaurer, Vider la base) et, en bas, l'utilisateur connecté, son rôle et un bouton « Se déconnecter ». |
| IHM-04 | Sur la fiche et le formulaire de chaque entité, le **type de l'entité** (« Personne », « Bureau »…) est affiché au-dessus du titre, et mène à la liste de ce type. |
| IHM-05 | Sur les fiches, un champ vide est affiché « — » ; les entités liées sont affichées sous forme de pastilles cliquables. |
| IHM-06 | Les actions de ligne des listes (Modifier, Supprimer) sont discrètes et restent sur une seule ligne. |
| IHM-07 | Les messages de succès et d'erreur (import HAL, restauration, vidage) s'affichent en tête de la page d'arrivée. |
| IHM-08 | Sur petit écran, la barre latérale devient un en-tête avec un bouton « Menu ». |
| IHM-09 | Style : titres à empattement (Source Serif 4), texte en Inter, barre latérale bleu nuit, accents dorés, contenu sur une feuille blanche. Tout le style tient dans une feuille de style unique posée sur Bootstrap. |

## 12. Connexion et rôles

| Id | Exigence |
|---|---|
| SEC-01 | Toutes les pages demandent une connexion, sauf la page de connexion elle-même. Un utilisateur non connecté est renvoyé vers `/login` ; après connexion, il arrive sur l'accueil. |
| SEC-02 | Deux rôles : **LECTEUR** et **GESTIONNAIRE**. Un gestionnaire a tous les droits d'un lecteur. |
| SEC-03 | Un **lecteur** consulte toutes les listes et fiches, utilise la recherche et télécharge le rapport Markdown. |
| SEC-04 | Seul un **gestionnaire** peut : ouvrir les formulaires de création et de modification ; enregistrer et supprimer ; lancer les imports HAL ; télécharger les exports JSON (extraction en masse de données personnelles) ; restaurer ; vider la base. |
| SEC-05 | Ces droits sont **vérifiés par le serveur** : une action interdite renvoie une page « Accès refusé » (erreur 403), même si l'URL est saisie à la main. |
| SEC-06 | Les boutons d'action interdits au rôle de l'utilisateur ne sont pas affichés (Ajouter, Modifier, Supprimer, imports HAL, Export JSON, Restaurer, Vider la base). |
| SEC-07 | Tous les formulaires d'écriture sont protégés contre la falsification de requêtes intersites (jeton CSRF) : une requête sans jeton valide est refusée. |
| SEC-08 | La déconnexion ramène à la page de connexion, avec un message de confirmation. |
| SEC-09 | Les comptes sont déclarés dans la configuration (`securite.utilisateurs` : identifiant, mot de passe, rôle). Les mots de passe sont stockés hachés (bcrypt) ; un mot de passe en clair n'est accepté que pour le développement et provoque un avertissement au démarrage. |
| SEC-10 | Le remplacement des comptes locaux par le SSO de l'établissement (CAS, OIDC) ne doit pas modifier les règles d'accès SEC-02 à SEC-07. |

## 13. Exigences techniques

| Id | Exigence |
|---|---|
| TEC-01 | Java 21, Spring Boot 4.1, Spring Data JPA (Hibernate 7), Thymeleaf, Bootstrap 5, MariaDB 11. |
| TEC-02 | La base de développement se lance avec `docker compose up -d` (base `hr`, utilisateur `hr`/`hr`, port 3306), l'application avec `mvn spring-boot:run` (port 8080). |
| TEC-03 | Le schéma de la base est créé et mis à jour automatiquement par Hibernate au démarrage. |
| TEC-04 | Les relations sont envoyées par les formulaires sous forme d'identifiants (`xxxId`, `xxxIds`) et résolues explicitement par chaque contrôleur. |
| TEC-05 | Aucune collection non renvoyée par un formulaire ne doit être en cascade avec suppression des orphelins, sous peine d'effacer des données à chaque enregistrement (voir `CLAUDE.md`). |
| TEC-06 | La recherche (RECH-04), l'export (EXP-09) et la restauration s'appuient sur la réflexion et le mapping Hibernate : ils ne contiennent aucun nom de champ, de table ou de colonne écrit en dur. |
| TEC-07 | Toute règle métier non triviale est couverte par un test automatisé (`mvn test`). |
| TEC-08 | L'architecture est documentée dans `doc/latex/architecture.tex` (PDF généré par `latexmk -pdf`). |

## 14. Limites connues

- Comptes locaux seulement, déclarés dans la configuration : pas encore de SSO ni de gestion des comptes depuis l'interface (SEC-09, SEC-10).
- Pas de migrations de schéma versionnées : à prévoir avant une mise en production.
- Le formulaire de poste propose tous les grades, sans les filtrer selon l'employeur choisi.
- La recherche ne traverse pas les relations : chercher le nom d'une équipe fait remonter l'équipe, pas ses membres.
- L'import HAL est additif (HAL-09) et s'exécute pendant que l'utilisateur attend.
- La nationalité, le pays de naissance, la thèse et les financements n'ont pas de champ dans le modèle (CSV-09).
- Le rapport Markdown est nommé avec la date seule, sans l'heure.
