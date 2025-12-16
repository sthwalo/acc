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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
            )
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/v1/health", "/api/v1/health/**").permitAll()
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .requestMatchers("/api/docs/**", "/api/swagger-ui/**").permitAll()
                .requestMatchers("/api/v1/classification/**").permitAll()  // TEMPORARY: Allow classification endpoints for testing
                .requestMatchers("/api/v1/import/**").permitAll()  // TEMPORARY: Allow import endpoints for testing
                .requestMatchers("/api/v1/industries/**").permitAll()  // Allow industries for company setup
                // NOTE: Companies endpoints should require authentication. The previous
                // temporary permitAll was removed so that endpoints like
                // GET /api/v1/companies/user are only accessible to authenticated users.
                .requestMatchers("/api/v1/plans/**").permitAll() // Allow plans to be fetched without authentication (for registration)
                .requestMatchers("/api/plans/**").permitAll() // Allow plans (non-versioned) to be fetched without authentication
                .requestMatchers("/api/v1/paypal/webhook").permitAll() // Allow PayPal sandbox webhooks
                .requestMatchers("/api/paypal/webhook").permitAll() // Allow non-versioned webhook path
                .requestMatchers("/api/v1/paypal/**").permitAll() // Allow PayPal create/capture endpoints for unauthenticated registration flow
                .requestMatchers("/api/paypal/**").permitAll() // Allow non-versioned PayPal endpoints

                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                // Fallback
                .anyRequest().permitAll()  // TEMPORARY: Allow all other requests for testing
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