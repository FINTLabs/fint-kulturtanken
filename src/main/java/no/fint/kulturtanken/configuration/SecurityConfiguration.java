package no.fint.kulturtanken.configuration;

import no.fint.kulturtanken.util.CurrentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${fint.basic.username}")
    private String username;

    @Value("${fint.basic.password}")
    private String password;

    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests(
                authorizeRequests ->
                        authorizeRequests
                                .anyRequest().permitAll()
        );
    }

    @Bean
    public Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(username, password, Collections.emptyList());
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
                                                                 OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .password().refreshToken().build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        authorizedClientManager.setContextAttributesMapper(contextAttributesMapper());

        return authorizedClientManager;
    }

    private Function<OAuth2AuthorizeRequest, Map<String, Object>> contextAttributesMapper() {
        return authorizeRequest -> {
            Map<String, Object> contextAttributes = new HashMap<>();
            contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, authorizeRequest.getAttribute(OAuth2ParameterNames.USERNAME));
            contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, authorizeRequest.getAttribute(OAuth2ParameterNames.PASSWORD));
            return contextAttributes;
        };
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder, OAuth2AuthorizedClientManager authorizedClientManager) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();

        ServletOAuth2AuthorizedClientExchangeFilterFunction authorizedClientExchangeFilterFunction =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        return builder
                .exchangeStrategies(exchangeStrategies)
                .filter(authorizedClientExchangeFilterFunction)
                .build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }
}