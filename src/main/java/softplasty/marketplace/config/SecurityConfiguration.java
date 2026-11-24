package softplasty.marketplace.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import softplasty.marketplace.security.AccountAccessAuthorizationManager;
import softplasty.marketplace.security.DatabaseJwtAuthenticationConverter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabaseJwtAuthenticationConverter jwtAuthenticationConverter,
            AccountAccessAuthorizationManager accountAccessAuthorizationManager
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(org.springframework.web.cors.CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/usuarios/restablecimientos",
                                "/api/usuarios/restablecimientos/canjear",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/mias", "/api/publicaciones/mias/**")
                        .access(accountAccessAuthorizationManager)
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones", "/api/publicaciones/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/*/resenas", "/api/servicios/tarifas")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/**").permitAll()
                        .anyRequest().access(accountAccessAuthorizationManager)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemAuthenticationEntryPoint())
                        .accessDeniedHandler(problemAccessDeniedHandler())
                )
                .build();
    }

    private AuthenticationEntryPoint problemAuthenticationEntryPoint() {
        return (request, response, exception) ->
                writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "No autenticado", "Se requiere una autenticación válida.");
    }

    private AccessDeniedHandler problemAccessDeniedHandler() {
        return (request, response, exception) ->
                writeProblem(response, HttpServletResponse.SC_FORBIDDEN,
                        "Acceso denegado", "La cuenta no tiene permiso para esta operación.");
    }

    private void writeProblem(
            HttpServletResponse response,
            int status,
            String title,
            String detail
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s"}
                """.formatted(title, status, detail);
        response.getWriter().write(body);
    }
}
