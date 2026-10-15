-- Base de développement sur la MariaDB du poste (Homebrew), une seule fois :
--   brew install mariadb && brew services start mariadb
--   sudo mariadb < scripts/creer_base_dev.sql
-- Mêmes identifiants que src/main/resources/application.yml (hr/hr, port 3306).
-- Ne touche pas aux autres bases du serveur. Peut être relancé sans effet.
CREATE DATABASE IF NOT EXISTS hr CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- JDBC se connecte en TCP ; selon la résolution, l'hôte vu est localhost, 127.0.0.1 ou ::1.
CREATE USER IF NOT EXISTS 'hr'@'localhost' IDENTIFIED BY 'hr';
CREATE USER IF NOT EXISTS 'hr'@'127.0.0.1' IDENTIFIED BY 'hr';
CREATE USER IF NOT EXISTS 'hr'@'::1' IDENTIFIED BY 'hr';
GRANT ALL ON hr.* TO 'hr'@'localhost';
GRANT ALL ON hr.* TO 'hr'@'127.0.0.1';
GRANT ALL ON hr.* TO 'hr'@'::1';
