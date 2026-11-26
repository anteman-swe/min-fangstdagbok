package se.fiskeexperten.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Inloggning med Spring Security.
 *
 * Användarna ligger i databasen. Spring Security hittar AnvandarService (en UserDetailsService)
 * och PasswordEncoder-bönan nedan automatiskt och sätter ihop inloggningen av dem.
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
}
