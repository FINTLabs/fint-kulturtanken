package no.fint.kulturtanken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties("kulturtanken")
public class KulturtankenProperties {

    private Map<String, Organisation> organisations = new HashMap<>();

    @Data
    public static class Organisation {

        private String name, source, username, password;
        private Boolean groups;
    }
}
