package se.fiskeexperten.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Inloggning med Spring Security.
 *
 * Användarna ligger i minnet tills databasen är på plats (Trello: "Datamodell").
 * Då byts {@link #userDetailsManager} mot en implementation som läser från databasen –
 * resten av klasserna behöver inte ändras.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterKedja(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(regler -> regler
                        // Öppna sidor: inloggning, registrering, felsidor och statiska filer
                        .requestMatchers("/", "/registrera", "/error", "/error/**",
                                "/css/**", "/js/**", "/favicon.ico").permitAll()
                        // Allt annat (karta, fångster, bilder, Om-sidan) kräver inloggning
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("losenord")
                        .defaultSuccessUrl("/karta", true)
                        .failureUrl("/?fel"))
                .logout(logout -> logout
                        .logoutUrl("/logga-ut") // kräver POST (CSRF-skyddat)
                        .logoutSuccessUrl("/?utloggad"))
                .csrf(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // BCrypt
    }

    /**
     * Användare i minnet. Ett demokonto läggs in vid start så att man slipper registrera
     * sig på nytt varje gång appen startar om (lämna e-post tom i application.properties
     * för att stänga av det).
     */
    @Bean
    public UserDetailsManager userDetailsManager(PasswordEncoder encoder,
                                                 @Value("${fiskeexperten.demo.email:}") String demoEpost,
                                                 @Value("${fiskeexperten.demo.losenord:}") String demoLosenord) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        if (!demoEpost.isBlank() && !demoLosenord.isBlank()) {
            manager.createUser(User.withUsername(demoEpost.trim().toLowerCase())
                    .password(encoder.encode(demoLosenord))
                    .roles("USER")
                    .build());
        }
        return manager;
    }
}
