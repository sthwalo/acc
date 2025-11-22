package fin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
//import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Spring Security Configuration with JWT authentication
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${FIN_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()  // Allow auth endpoints without authentication
                .requestMatchers("/api/v1/companies").permitAll()  // Allow general companies endpoint without auth
                .requestMatchers("/api/v1/companies/user").permitAll()  // TEMPORARY: Allow testing without auth
                .requestMatchers("/api/v1/companies/fiscal-periods/all").permitAll()  // TEMPORARY: Allow fiscal periods testing
                .requestMatchers("/api/v1/companies/*/fiscal-periods").permitAll()  // Allow fiscal periods for any company
                .requestMatchers("/api/v1/companies/*/fiscal-periods/*").permitAll()  // Allow specific fiscal period operations
                .requestMatchers("/api/v1/companies/*/fiscal-periods/**").permitAll()  // Allow ALL fiscal period operations (including sub-paths)
                .requestMatchers("/api/v1/companies/fiscal-periods/*/close").permitAll()  // Allow fiscal period close operations
                .requestMatchers("/api/v1/companies/*/fiscal-periods/*/transactions").permitAll()  // Allow transactions for any company/fiscal period
                .requestMatchers("/api/v1/companies/*/fiscal-periods/*/imports/**").permitAll()  // Allow file import operations for companies/fiscal-periods
                .requestMatchers("/api/v1/import/**").permitAll()  // Allow file import operations
                .requestMatchers("/api/v1/companies/*/data-management/**").permitAll()  // TEMPORARY: Allow data management endpoints for testing
                .requestMatchers("/error").permitAll()  // Allow error pages
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.disable())
            .sessionManagement(session -> session.disable())
            .addFilterBefore(jwtAuthenticationFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}