package no.fint.kulturtanken.repository;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Enhet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Repository
public class NsrRepository {

    private final RestTemplate restTemplate;

    @Value("${nsr.endpoints.unit}")
    private String unitEndpoint;

    public NsrRepository(@Qualifier("restTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Enhet getUnit(String orgId) {
        return restTemplate.getForObject(unitEndpoint, Enhet.class, orgId);
    }
}
