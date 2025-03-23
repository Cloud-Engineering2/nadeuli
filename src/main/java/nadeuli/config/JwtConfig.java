package nadeuli.config;

import jakarta.annotation.PostConstruct;
import nadeuli.common.util.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secretKey;

    @PostConstruct
    public void initJwtUtils() {
        JwtUtils.init(secretKey);
    }
}