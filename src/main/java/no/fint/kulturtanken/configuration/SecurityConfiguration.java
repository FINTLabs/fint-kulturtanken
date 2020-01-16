package no.fint.kulturtanken.configuration;

import no.fint.kulturtanken.util.CurrentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${fint.basic.username}")
    private String username;

    @Value("${fint.basic.password}")
    private String password;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .requestMatchers(EndpointRequest.to("health")).permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .httpBasic();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .inMemoryAuthentication()
                .passwordEncoder(passwordEncoder())
                .withUser(username)
                .password(passwordEncoder().encode(password))
                .roles("USER");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
                                                                 OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .password().refreshToken().build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);

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
    @RequestScope
    public CurrentRequest currentRequest() {
        return new CurrentRequest();
    }

    @Bean
    @RequestScope
    public OAuth2AccessToken oAuth2AccessToken(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
                                               CurrentRequest currentRequest,
                                               KulturtankenProperties kulturtankenProperties) {

        String orgId = currentRequest.getOrgId();

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(orgId)
                .principal(currentRequest.getPrincipal())
                .attributes(attrs -> {
                    attrs.put(OAuth2ParameterNames.USERNAME, kulturtankenProperties.getOrganisations().get(orgId).getUsername());
                    attrs.put(OAuth2ParameterNames.PASSWORD, kulturtankenProperties.getOrganisations().get(orgId).getPassword());
                }).build();

        OAuth2AuthorizedClient authorizedClient = oAuth2AuthorizedClientManager.authorize(authorizeRequest);

        return (authorizedClient != null ? authorizedClient.getAccessToken() : null);
    }

    @Bean
    @RequestScope
    public RestTemplate oAuth2RestTemplate(RestTemplateBuilder restTemplateBuilder, OAuth2AccessToken oAuth2AccessToken) {
        ClientHttpRequestInterceptor requestInterceptor = (oAuth2AccessToken != null ?
                getBearerTokenInterceptor(oAuth2AccessToken.getTokenValue()) : getNoTokenInterceptor());
        return restTemplateBuilder.interceptors(requestInterceptor).build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    private ClientHttpRequestInterceptor getBearerTokenInterceptor(String accessToken) {
        return (request, bytes, execution) -> {
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            return execution.execute(request, bytes);
        };
    }

    private ClientHttpRequestInterceptor getNoTokenInterceptor() {
        return (request, bytes, execution) -> {
            throw new IllegalStateException("Can't access FINT API without access token");
        };
    }
}
