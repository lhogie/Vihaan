#!/bin/bash
# Installation initiale du serveur (Debian 13), à lancer en root depuis une copie de deploy/ :
#   scp -r deploy admin.i3s:/tmp/hr-deploy && ssh -t admin.i3s sudo bash /tmp/hr-deploy/installer.sh
#   (ou, avec le mot de passe root : ssh -t admin.i3s su -c "'bash /tmp/hr-deploy/installer.sh'")
# Peut être relancé : les paquets, le service, le site nginx, la sauvegarde et le message de
# connexion ssh sont réinstallés,
# mais la base et /etc/hr/application.yml (mots de passe) ne sont créés qu'une fois.
set -euo pipefail
# « su -c » garde le PATH de l'utilisateur, sans /usr/sbin (useradd, nginx, visudo...).
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
cd "$(dirname "$0")"

apt-get update
apt-get install -y openjdk-21-jre-headless mariadb-server nginx certbot python3-certbot-nginx apache2-utils w3m

id hr &>/dev/null || useradd --system --home-dir /opt/hr --shell /usr/sbin/nologin hr
install -d -o hr -g hr -m 750 /opt/hr
install -d -o root -g hr -m 750 /etc/hr
install -d -o root -g root -m 700 /var/backups/hr

if [ ! -f /etc/hr/application.yml ]; then
    # Mot de passe de la base : aléatoire, seule l'application le connaît.
    mdp_base=$(head -c 24 /dev/urandom | base64 | tr -d '/+=')
    mariadb -e "CREATE DATABASE IF NOT EXISTS hr CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
        CREATE USER IF NOT EXISTS 'hr'@'localhost' IDENTIFIED BY '$mdp_base';
        ALTER USER 'hr'@'localhost' IDENTIFIED BY '$mdp_base';
        GRANT ALL ON hr.* TO 'hr'@'localhost';"

    # Comptes de l'application : seuls leurs hash bcrypt sont écrits sur disque.
    hash() {
        local mdp confirmation
        while true; do
            read -rsp "Mot de passe du compte « $1 » : " mdp; echo >&2
            read -rsp "Confirmation : " confirmation; echo >&2
            if [ -n "$mdp" ] && [ "$mdp" = "$confirmation" ]; then break; fi
            echo "Vide ou différent, recommencer." >&2
        done
        printf '%s' "$mdp" | htpasswd -niBC 12 "$1" | head -1 | cut -d: -f2
    }
    hash_gestionnaire=$(hash gestionnaire)
    hash_lecteur=$(hash lecteur)

    sed -e "s|@MDP_BASE@|$mdp_base|" \
        -e "s|@HASH_GESTIONNAIRE@|$hash_gestionnaire|" \
        -e "s|@HASH_LECTEUR@|$hash_lecteur|" \
        application-prod.yml > /etc/hr/application.yml
    chown root:hr /etc/hr/application.yml
    chmod 640 /etc/hr/application.yml
fi

install -m 644 hr.service /etc/systemd/system/hr.service
systemctl daemon-reload
systemctl enable hr

install -m 644 nginx-hr.conf /etc/nginx/sites-available/hr
ln -sf /etc/nginx/sites-available/hr /etc/nginx/sites-enabled/hr
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

install -m 755 hr-sauvegarde /etc/cron.daily/hr-sauvegarde
install -m 755 hr-motd /etc/update-motd.d/50-hr

echo
echo "Installation terminée. Étapes suivantes :"
echo "  1. depuis le poste de développement : deploy/deployer.sh   (copie le jar et démarre le service)"
echo "  2. ici : sudo certbot --nginx -d admin.i3s.unice.fr        (HTTPS)"
