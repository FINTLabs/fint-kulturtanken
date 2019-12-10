package no.fint.kulturtanken.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties("kulturtanken")
public class KulturtankenProperties {

    private Map<String, Organisation> organisations = new HashMap<>();

    @Data
    public static class Organisation {

        @JsonProperty("navn")
        private String name;

        @JsonProperty("organisasjonsnummer")
        private String organisationNumber;

        @JsonIgnore
        private String source, username, password;

        @JsonProperty("grupper")
        private Boolean groups;

        @JsonProperty("uri")
        private URI uri;
    }
}
