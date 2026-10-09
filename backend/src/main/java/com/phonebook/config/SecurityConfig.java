package com.phonebook.config;

import com.phonebook.security.JwtAuthenticationEntryPoint;
import com.phonebook.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationEntryPoint authenticationEntryPoint;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(
      JwtAuthenticationEntryPoint authenticationEntryPoint, JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults())
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
        .authorizeHttpRequests(
            auth ->
                auth
                    .requestMatchers("/health", "/auth/login", "/auth/logout")
                    .permitAll()
                    .requestMatchers("/auth/me", "/stats", "/contacts", "/contacts/**")
                    .authenticated()
                    // Unknown routes fall through to the DispatcherServlet,
                    // which throws NoHandlerFoundException -> 404 (FastAPI parity).
                    .anyRequest()
                    .permitAll());

    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /** Matches the Python backend's permissive CORS policy (development convenience). */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOriginPattern("*");
    configuration.addAllowedMethod("*");
    configuration.addAllowedHeader("*");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /** BCrypt - hashes created by the Python backend verify correctly. */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
