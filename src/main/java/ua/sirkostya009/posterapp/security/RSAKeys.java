package ua.sirkostya009.posterapp.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@ConfigurationProperties(prefix = "rsa")
public @Data class RSAKeys {
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
}
