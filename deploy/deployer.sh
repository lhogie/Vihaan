#!/bin/bash
# Construit le jar (tests compris : la base de développement doit tourner) et le met en
# service sur le serveur. Le jar précédent est gardé en /opt/hr/hr.jar.precedent.
#   deploy/deployer.sh [hôte]        (par défaut admin.i3s)
# Retour arrière : ssh -t admin.i3s 'sudo cp /opt/hr/hr.jar.precedent /opt/hr/hr.jar && sudo systemctl restart hr'
set -euo pipefail
hote=${1:-admin.i3s}
cd "$(dirname "$0")/.."

mvn -q package
scp target/hr-*.jar "$hote:/tmp/hr.jar"
ssh -t "$hote" '
    set -e
    if [ -f /opt/hr/hr.jar ]; then sudo cp -p /opt/hr/hr.jar /opt/hr/hr.jar.precedent; fi
    sudo install -o hr -g hr -m 640 /tmp/hr.jar /opt/hr/hr.jar
    rm /tmp/hr.jar
    debut=$(date "+%F %T")
    sudo systemctl restart hr
    echo "Démarrage..."
    for i in $(seq 60); do
        if sudo journalctl -u hr --since "$debut" | grep -q "Started HrApplication"; then
            echo "Application démarrée."; exit 0
        fi
        sleep 2
    done
    echo "Pas démarrée après 2 minutes :"; sudo journalctl -u hr -n 30 --no-pager; exit 1
'
