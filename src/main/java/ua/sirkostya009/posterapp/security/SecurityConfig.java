package ua.sirkostya009.posterapp.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import ua.sirkostya009.posterapp.service.UserService;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.SUB;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RSAKeys keys;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserService service) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeRequests(authorize -> authorize
                        .antMatchers("/api/v1/register/**", "/api/v1/users/login").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(STATELESS))
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .userDetailsService(service)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(
                new RSAKey.Builder(keys.getPublicKey()).privateKey(keys.getPrivateKey()).build()
        )));
    }

    @Bean
    public JwtDecoder jwtDecoder(UserService service) {
        var decoder = NimbusJwtDecoder.withPublicKey(keys.getPublicKey()).build();
        decoder.setJwtValidator(delegatingValidator(service));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> delegatingValidator(UserService service) {
        return new DelegatingOAuth2TokenValidator<>(
                subjectValidator(service),
                new JwtTimestampValidator()
        );
    }

    private OAuth2TokenValidator<Jwt> subjectValidator(UserService service) {
        return new JwtClaimValidator<String>(SUB, sub -> service.findByUsername(sub).isEnabled());
    }

}
