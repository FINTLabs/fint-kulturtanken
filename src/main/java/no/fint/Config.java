package no.fint;

import no.fint.oauth.OAuthConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Import(OAuthConfig.class)
@Configuration
public class Config {

    @Value("${fint.base-uri}")
    private String baseUri;

    @Bean
    public OAuth2RestTemplate restTemplate(OAuth2RestTemplate oAuth2RestTemplate) {
        oAuth2RestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(baseUri));
        return oAuth2RestTemplate;
    }
}
