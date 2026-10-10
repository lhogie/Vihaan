package fr.lab.hr.securite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Droits d'accès, vérifiés côté serveur sur les URL :
 * <ul>
 * <li>sans connexion : seulement la page de connexion et la feuille de style ;</li>
 * <li>{@link Role#LECTEUR} : toutes les pages en lecture (GET), recherche et rapport Markdown ;</li>
 * <li>{@link Role#GESTIONNAIRE} : en plus, les formulaires de création et de modification,
 * les exports JSON (extraction en masse de données personnelles), la restauration, le vidage
 * et toutes les requêtes POST (enregistrement, suppression, import HAL...).</li>
 * </ul>
 * Masquer les boutons dans les pages (voir {@code NavigationAdvice}) n'est qu'un confort :
 * c'est cette configuration qui protège. La protection CSRF de Spring Security est active ;
 * Thymeleaf ajoute le jeton à tous les formulaires utilisant {@code th:action}.
 * <p>
 * Les comptes sont locaux pour l'instant ({@link ProprietesSecurite}). Le passage à un SSO
 * institutionnel (CAS, OIDC) remplacera {@link #utilisateurs} et le formulaire de connexion,
 * sans toucher aux règles ci-dessous.
 */
@Configuration
@EnableConfigurationProperties(ProprietesSecurite.class)
public class ConfigurationSecurite {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationSecurite.class);

    @Bean
    SecurityFilterChain securite(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(acces -> acces
                        .requestMatchers("/login", "/css/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/*/new", "/*/*/edit", "/export/**", "/import", "/vider")
                        .hasRole(Role.GESTIONNAIRE.name())
                        .requestMatchers(HttpMethod.GET, "/**").hasRole(Role.LECTEUR.name())
                        .anyRequest().hasRole(Role.GESTIONNAIRE.name()))
                .formLogin(connexion -> connexion.loginPage("/login").defaultSuccessUrl("/", false).permitAll())
                .logout(deconnexion -> deconnexion.logoutSuccessUrl("/login?deconnecte").permitAll());
        return http.build();
    }

    /** Un gestionnaire peut tout ce que peut un lecteur. */
    @Bean
    static RoleHierarchy hierarchieDesRoles() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(Role.GESTIONNAIRE.name()).implies(Role.LECTEUR.name())
                .build();
    }

    @Bean
    PasswordEncoder encodeurMotsDePasse() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService utilisateurs(ProprietesSecurite proprietes) {
        InMemoryUserDetailsManager comptes = new InMemoryUserDetailsManager();
        for (ProprietesSecurite.Utilisateur u : proprietes.utilisateurs()) {
            if (u.motDePasse() != null && u.motDePasse().startsWith("{noop}")) {
                LOG.warn("Le compte « {} » a un mot de passe en clair ({noop}) : à remplacer par un hash {bcrypt} "
                        + "avant toute mise à disposition.", u.identifiant());
            }
            comptes.createUser(User.withUsername(u.identifiant()).password(u.motDePasse())
                    .roles(u.role().name()).build());
        }
        if (proprietes.utilisateurs().isEmpty()) {
            LOG.warn("Aucun compte déclaré sous securite.utilisateurs : personne ne pourra se connecter.");
        }
        return comptes;
    }
}
