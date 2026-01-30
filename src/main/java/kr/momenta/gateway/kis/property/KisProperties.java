package kr.momenta.gateway.kis.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "kis")
@Configuration
@Data
public class KisProperties {

    private String baseUrl;
    private String appKey;
    private String appSecret;
}
