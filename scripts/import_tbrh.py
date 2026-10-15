#!/usr/bin/env python3
"""Importe les exports CSV du tableau de bord RH de l'I3S (dossier tbrh) dans la base.

Usage : python3 scripts/import_tbrh.py <dossier_csv> [--dry-run]

En production (admin.i3s, MariaDB locale), en root pour l'accès à la base :
  su -c 'python3 scripts/import_tbrh.py <dossier_csv>'

Crée les personnes, leurs postes (employeur + grade + dates), leurs
structures (pôle sous I3S, équipe sous pôle) et leurs bureaux (bâtiment =
site de recherche). Les structures, employeurs, grades, bâtiments et bureaux
déjà présents en base (même nom, sans tenir compte de la casse) sont
réutilisés. Refuse de tourner si la table personne n'est pas vide, pour ne pas
créer de doublons.

Le SQL est exécuté dans une seule transaction, par la commande donnée dans la
variable d'environnement HR_MARIADB ; par défaut, la MariaDB locale : en root
« mariadb hr » (accès par socket unix, cas du serveur), sinon avec le compte de
développement hr/hr.
"""

import csv
import os
import re
import shlex
import subprocess
import sys
import unicodedata
from datetime import date
from pathlib import Path

DB_CMD = shlex.split(os.environ.get("HR_MARIADB") or (
    "mariadb hr" if os.geteuid() == 0 else "mariadb -uhr -phr hr"))

# Catégorie de chaque fichier : grade utilisé quand le fichier ne donne pas de corps/grade,
# et employeur par défaut quand aucune colonne d'organisme n'est remplie.
FICHIERS = {
    "personneI3S_ATER.csv": ("ATER", None),
    "personneI3S_C.csv": (None, None),
    "personneI3S_CDD.csv": ("CDD", None),
    "personneI3S_DOC.csv": ("DOC", None),
    "personneI3S_EC.csv": (None, "UCA"),
    "personneI3S_IT.csv": (None, None),
    "personneI3S_POSTDOC.csv": ("POSTDOC", None),
    "stagiaire.csv": ("STAGIAIRE", None),
    "visiteur.csv": ("VISITEUR", None),
}

# Dans personneI3S_EC.csv, dix colonnes sont décalées par rapport à l'en-tête
# (vérifié sur les valeurs : la colonne "mobile" contient les sites, "site web"
# les e-mails, etc.). Nom d'en-tête -> nom réel de la donnée.
REMAP_EC = {
    "mobile": "site recherche",
    "badge": "autre site recherche",
    "site recherche": "téléphone professionnel 1",
    "autre site recherche": "téléphone professionnel 2",
    "téléphone professionnel 1": "mobile",
    "téléphone professionnel 2": "bureau",
    "numéro de fax": "badge",
    "adresse mail": "site web",
    "bureau": "numéro de fax",
    "site web": "adresse mail",
}

# Corps/grade du fichier -> (code, libellé). Les codes identiques au catalogue
# initial (IE, IR, AI...) réutilisent la ligne existante.
GRADES = {
    "DR1": ("DR1", "Directeur de recherche 1re classe"),
    "DR2": ("DR2", "Directeur de recherche 2e classe"),
    "DRCE": ("DRCE", "Directeur de recherche classe exceptionnelle"),
    "CR1": ("CR1", "Chargé de recherche 1re classe"),
    "CR2": ("CR2", "Chargé de recherche 2e classe"),
    "CRCN": ("CRCN", "Chargé de recherche classe normale"),
    "CR0 (CRHC)": ("CRHC", "Chargé de recherche hors classe"),
    "CRHC": ("CRHC", "Chargé de recherche hors classe"),
    "ISFP": ("ISFP", "Inria Starting Faculty Position"),
    "CPJ": ("CPJ", "Chaire de professeur junior"),
    "MCN": ("MCN", "Maître de conférences classe normale"),
    "MCH": ("MCH", "Maître de conférences hors classe"),
    "MCE": ("MCE", "Maître de conférences échelon exceptionnel"),
    "PR1": ("PR1", "Professeur des universités 1re classe"),
    "PR2": ("PR2", "Professeur des universités 2e classe"),
    "PREM": ("PREM", "Professeur émérite"),
    "PRCE": ("PRCE", "Professeur certifié"),
    "AI": ("AI", None), "ASI": ("AI", None), "AI/ASI": ("AI", None),
    "IE": ("IE", None), "IE/IGE": ("IE", None),
    "IR": ("IR", None), "IR/IGR": ("IR", None),
    "IR1": ("IR1", "Ingénieur de recherche 1re classe"),
    "AJT/ADT": ("AJT", "Adjoint technique"),
    "TCS": ("TCS", "Technicien classe supérieure"),
    "ATER": ("ATER", "Attaché temporaire d'enseignement et de recherche"),
    "CDD": ("CDD", "Contrat à durée déterminée"),
    "DOC": ("DOC", "Doctorant"),
    "POSTDOC": ("POSTDOC", "Post-doctorant"),
    "STAGIAIRE": ("STAGIAIRE", "Stagiaire"),
    "VISITEUR": ("VISITEUR", "Visiteur"),
}

# Noms d'organismes équivalents (UNS est devenue UCA en 2020).
EMPLOYEURS_ALIAS = {
    "UNS": "UCA", "UNICA": "UCA", "UNIVERSITE DE NICE": "UCA", "UNICA/POLYTECH": "UCA",
}
EMPLOYEURS_PUBLICS = re.compile(
    r"CNRS|INRIA|^UCA$|UNIV|ECOLE|ENS |ONERA|CEA|MESRI|IDEX|3IA|DS4H|INSTITUT|CAPES", re.I)
# Sites de recherche connus, utilisés comme bâtiments. Les valeurs "autre site"
# du fichier sont trop hétérogènes (téléphones, "en télétravail"...) pour en faire des bâtiments.
SITES = {"ALGORITHMES", "TEMPLIERS", "INRIA"}
DATES_INCONNUES = {"0000-00-00", "9999-12-31", "2099-12-31"}
MOIS = {"janvier": 1, "fevrier": 2, "mars": 3, "avril": 4, "mai": 5, "juin": 6, "juillet": 7,
        "aout": 8, "septembre": 9, "octobre": 10, "novembre": 11, "decembre": 12}


def cle(s):
    return unicodedata.normalize("NFKD", s or "").encode("ascii", "ignore").decode().upper().strip()


def val(d, *noms):
    for n in noms:
        v = (d.get(n) or "").strip()
        if v:
            return v
    return None


def avec_autre(d, nom, nom_autre):
    """Valeur de `nom`, remplacée par celle de `nom_autre` quand elle vaut "autre"."""
    v = val(d, nom)
    if v and v.lower() == "autre":
        return val(d, nom_autre)
    return v


def parse_date(s):
    """Date complète (jj/mm/aaaa ou aaaa-mm-jj) ; None si absente, sentinelle ou invalide."""
    if not s or s.strip() in DATES_INCONNUES:
        return None
    s = s.strip()
    for motif, ordre in ((r"(\d{1,2})/(\d{1,2})/(\d{4})$", "jma"), (r"(\d{4})-(\d{2})-(\d{2})$", "amj")):
        m = re.match(motif, s)
        if m:
            g = dict(zip(ordre, map(int, m.groups())))
            try:
                return date(g["a"], g["m"], g["j"])
            except ValueError:
                return None
    return None


def parse_date_partielle(s):
    """Comme parse_date, mais accepte aussi mm/aaaa, aaaa et "juin 2018" (premier jour de la période)."""
    d = parse_date(s)
    if d or not s:
        return d
    s = cle(s).lower()
    m = re.match(r"(\d{1,2})/(\d{4})$", s)
    if m and 1 <= int(m.group(1)) <= 12:
        return date(int(m.group(2)), int(m.group(1)), 1)
    m = re.match(r"([a-z]+) (\d{4})$", s)
    if m and m.group(1) in MOIS:
        return date(int(m.group(2)), MOIS[m.group(1)], 1)
    m = re.match(r"(\d{4})$", s)
    if m:
        return date(int(m.group(1)), 1, 1)
    return None


def telephone(s):
    if not s or len(re.sub(r"\D", "", s)) < 8:  # écarte les "(33) 0" laissés par défaut
        return None
    return s.strip()


def sql(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "b'1'" if v else "b'0'"
    if isinstance(v, int):
        return str(v)
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def requete(q):
    out = subprocess.run(DB_CMD + ["-N", "-e", q], check=True, capture_output=True, text=True).stdout
    return [ligne.split("\t") for ligne in out.splitlines() if ligne]


class Referentiel:
    """Lignes existantes d'une table, indexées par nom normalisé, plus celles à créer."""

    def __init__(self, table, colonne_nom, colonnes_extra=""):
        self.table = table
        self.index = {}
        for ligne in requete(f"SELECT id, {colonne_nom}{colonnes_extra} FROM {table}"):
            self.index.setdefault(cle(ligne[1]), int(ligne[0]))
        self.prochain = int(requete(f"SELECT COALESCE(MAX(id), 0) + 1 FROM {table}")[0][0])

    def obtenir(self, nom, creer):
        k = cle(nom)
        if k not in self.index:
            self.index[k] = self.prochain
            creer(self.prochain)
            self.prochain += 1
        return self.index[k]


def lire(dossier):
    for nom_fichier, (categorie, employeur_defaut) in FICHIERS.items():
        chemin = Path(dossier) / nom_fichier
        if not chemin.exists():
            print(f"absent, ignoré : {chemin}", file=sys.stderr)
            continue
        with open(chemin, encoding="utf-8", newline="") as f:
            lignes = list(csv.reader(f, delimiter=";"))
        entete = lignes[0]
        if nom_fichier == "personneI3S_EC.csv":
            entete = [REMAP_EC.get(h, h) for h in entete]
        for ligne in lignes[1:]:
            yield nom_fichier, categorie, employeur_defaut, dict(zip(entete, ligne))


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    dossier, dry_run = sys.argv[1], "--dry-run" in sys.argv

    if int(requete("SELECT COUNT(*) FROM personne")[0][0]) > 0:
        sys.exit("La table personne n'est pas vide : import annulé pour éviter les doublons.")

    out = ["START TRANSACTION;"]
    structures = Referentiel("structure", "nom")
    employeurs = Referentiel("employeur", "nom")
    grades = Referentiel("grade", "code")
    batiments = Referentiel("batiment", "nom")
    bureaux = Referentiel("bureau", "CONCAT(COALESCE(batiment_id, ''), '/', nom)")
    employeur_grade = {tuple(map(int, l)) for l in requete("SELECT employeurs_id, grades_id FROM employeur_grade")}

    parents = {}  # parent des structures créées par cet import
    poles = set()  # structures utilisées comme pôle : jamais déplacées

    def creer_structure(i, nom, parent_id):
        parents[i] = parent_id
        out.append(f"INSERT INTO structure (id, nom, structure_parente_id) VALUES ({i}, {sql(nom)}, {sql(parent_id)});")

    def structure(nom, parent_id):
        i = structures.obtenir(nom, lambda i: creer_structure(i, nom, parent_id))
        # Équipe d'abord vue sans pôle (rattachée à I3S), puis avec : on la range sous son pôle.
        if parents.get(i) == racine and i not in poles and parent_id not in (None, racine, i):
            parents[i] = parent_id
            out.append(f"UPDATE structure SET structure_parente_id = {parent_id} WHERE id = {i};")
        return i

    racine = None
    racine = structure("I3S", None)

    def employeur(nom):
        if not nom or "@" in nom or "recherche d'emploi" in nom.lower():
            return None
        nom = EMPLOYEURS_ALIAS.get(cle(nom), nom)
        public = bool(EMPLOYEURS_PUBLICS.search(nom))
        return employeurs.obtenir(nom, lambda i: out.append(
            f"INSERT INTO employeur (id, nom, est_public) VALUES ({i}, {sql(nom)}, {sql(public)});"))

    def grade(brut):
        if not brut or brut.lower() == "autre":
            return None
        code, libelle = GRADES.get(brut.strip().upper(), (brut.strip(), None))
        return grades.obtenir(code, lambda i: out.append(
            f"INSERT INTO grade (id, code, libelle) VALUES ({i}, {sql(code)}, {sql(libelle or code)});"))

    def bureau(numero, site):
        if not numero or cle(numero) in ("SANS", "0", "N/A"):
            return None
        batiment_id = None
        if site in SITES:
            batiment_id = batiments.obtenir(site, lambda i: out.append(
                f"INSERT INTO batiment (id, nom) VALUES ({i}, {sql(site)});"))
        return bureaux.obtenir(f"{batiment_id or ''}/{numero}", lambda i: out.append(
            f"INSERT INTO bureau (id, nom, batiment_id) VALUES ({i}, {sql(numero)}, {sql(batiment_id)});"))

    personne_id = int(requete("SELECT COALESCE(MAX(id), 0) + 1 FROM personne")[0][0])
    poste_id = int(requete("SELECT COALESCE(MAX(id), 0) + 1 FROM poste")[0][0])
    vus, stats = {}, {"personnes": 0, "postes": 0, "doublons": 0}

    for fichier, categorie, employeur_defaut, d in lire(dossier):
        nom, prenom = val(d, "nom usuel", "nom complet"), val(d, "prénom")
        if not nom or not prenom:
            continue
        naissance = parse_date(val(d, "date de naissance"))
        identite = (cle(nom), cle(prenom), naissance)

        # Structure : équipe sous son pôle, sinon le pôle, sinon rien.
        pole = val(d, "nom du pôle")
        equipe = avec_autre(d, "nom de l'équipe", "autre équipe") or avec_autre(
            d, "nom de l'équipe d'accueil", "autre équipe d'accueil") or val(d, "si autre équipe, préciser")
        pole_id = None
        if pole:
            pole_id = racine if cle(pole) == "LABORATOIRE" else structure(pole, racine)
            poles.add(pole_id)
        structure_id = pole_id
        if equipe and cle(equipe) not in ("LABORATOIRE", cle(pole)):
            structure_id = structure(equipe, pole_id or racine)

        emp = employeur(avec_autre(d, "organisme d'appartenance", "autre organisme")
                        or avec_autre(d, "organisme gestionnaire", "autre organisme gestionnaire")
                        or avec_autre(d, "nom organisme gestionnaire", "autre organisme gestionnaire")
                        or avec_autre(d, "organisme Gestionnaire", "autre organisme gestionnaire")
                        or avec_autre(d, "établissement", "autre établissement")
                        or employeur_defaut)
        grd = grade(avec_autre(d, "corps et grade", "autre corps et grade")
                    or avec_autre(d, "corps et grade", "autre corps/Grade")
                    or categorie)
        debut = parse_date(val(d, "date d'arrivée", "date arrivée"))
        fin = parse_date(val(d, "date de départ"))

        if identite in vus:
            # Même personne dans deux fichiers : un poste de plus, pas une nouvelle personne.
            pid = vus[identite]
            stats["doublons"] += 1
        else:
            pid = personne_id
            personne_id += 1
            vus[identite] = pid
            stats["personnes"] += 1
            attachement = cle(val(d, "type d'attachement", "autre type d'attachement"))
            quotite = val(d, "quotité temps partiel", "quotité temps partile", "quotité")
            nom_jf = val(d, "nom de jeune fille")
            section = avec_autre(d, "section CNRS", "autre section CNRS")
            site = val(d, "site recherche", "site de recherche")
            bur = bureau(val(d, "numéro de bureau", "numéro du bureau", "bureau"), site)
            out.append(
                "INSERT INTO personne (id, nom, prenom, nom_jeune_fille, date_naissance, ville_naissance,"
                " numero_badge, date_doctorat, esthdr, est_associe, est_collaborateur, quotite_travail,"
                " sectioncnrs, structure_id, bureau_principal_id) VALUES ("
                + ", ".join(sql(v) for v in (
                    pid, nom, prenom, nom_jf if nom_jf and cle(nom_jf) != cle(nom) else None,
                    naissance and naissance.isoformat(), val(d, "ville de naissance"),
                    val(d, "numéro de badge", "numéro de badge I3S", "numéro du badge I3S", "badge"),
                    (lambda x: x and x.isoformat())(parse_date_partielle(
                        val(d, "date du doctorat", "date doctorat", "date d'obtention du doctorat"))),
                    cle(val(d, "HDR")) == "OUI", "ASSOCI" in attachement, "COLLABORATEUR" in attachement,
                    int(quotite) if quotite and quotite.isdigit() else 100,
                    section, structure_id, bur))
                + ");")
            tels = {t for t in (telephone(val(d, c)) for c in (
                "téléphone professionnel 1", "téléphone professionnel 2", "téléphone travail 1",
                "téléphone travail 2", "mobile")) if t}
            for t in sorted(tels):
                out.append(f"INSERT INTO personne_telephone (personne_id, telephones) VALUES ({pid}, {sql(t)});")
            mail = val(d, "adresse mail", "email")
            if mail and "@" in mail:
                out.append(f"INSERT INTO personne_email (personne_id, emails) VALUES ({pid}, {sql(mail)});")
            web = val(d, "site web", "site Web")
            if web and len(web) > len("https://"):
                out.append(f"INSERT INTO personne_page_web (personne_id, pages_web) VALUES ({pid}, {sql(web)});")

        if emp or grd:
            out.append(
                "INSERT INTO poste (id, personne_id, employeur_id, grade_id, date_debut, date_fin) VALUES ("
                + ", ".join(sql(v) for v in (poste_id, pid, emp, grd,
                                             debut and debut.isoformat(), fin and fin.isoformat())) + ");")
            poste_id += 1
            stats["postes"] += 1
            if emp and grd and (emp, grd) not in employeur_grade:
                employeur_grade.add((emp, grd))
                out.append(f"INSERT INTO employeur_grade (employeurs_id, grades_id) VALUES ({emp}, {grd});")

    out.append("COMMIT;")
    script = "\n".join(out) + "\n"
    if dry_run:
        print(script)
    else:
        subprocess.run(DB_CMD, input=script, text=True, check=True)
    print(f"{stats['personnes']} personnes, {stats['postes']} postes"
          f" ({stats['doublons']} ligne(s) rattachée(s) à une personne déjà lue)", file=sys.stderr)


if __name__ == "__main__":
    main()
