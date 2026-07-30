package com.umrah.scanner.config;

import com.umrah.scanner.auth.infrastructure.JjwtAccessTokenIssuer;
import com.umrah.scanner.auth.infrastructure.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String API_V1 = "/api/v1";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JjwtAccessTokenIssuer accessTokenIssuer,
            ObjectMapper objectMapper,
            @Value("${app.dev-auth.enabled:false}") boolean devAuthEnabled)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, API_V1 + "/auth/google", API_V1 + "/auth/refresh", API_V1 + "/auth/logout")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, API_V1 + "/trips/**", API_V1 + "/companies/*/ratings")
                            .permitAll()
                            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health")
                            .permitAll();
                    // Independent of DevAuthController's own @Profile("dev") + flag check — this
                    // path is only ever open here too when the exact same flag is explicitly set.
                    if (devAuthEnabled) {
                        auth.requestMatchers(HttpMethod.POST, API_V1 + "/auth/dev-google-test").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJsonError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", ex.getMessage()))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJsonError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "Forbidden", ex.getMessage())))
                .addFilterBefore(new JwtAuthenticationFilter(accessTokenIssuer, objectMapper), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, ObjectMapper objectMapper, int status, String error, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("status", status, "error", error, "message", message)));
    }
}
