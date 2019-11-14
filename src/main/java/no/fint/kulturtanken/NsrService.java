package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Besoksadresse;
import no.fint.kulturtanken.model.Enhet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class NsrService {

    private final RestTemplate restTemplate;

    @Value("${nsr.endpoints.unit}")
    private String unitEndpoint;

    public NsrService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "schools")
    public Besoksadresse getVisitingAddress(String schoolId) {
        Enhet school = restTemplate.getForObject(unitEndpoint, Enhet.class, schoolId);

        Besoksadresse visitingAddress = new Besoksadresse();

        Optional<Enhet.Adresse> address = Optional.ofNullable(school).map(Enhet::getBesoksadresse);
        address.map(Enhet.Adresse::getAdresselinje).map(Collections::singletonList).ifPresent(visitingAddress::setAdresselinje);
        address.map(Enhet.Adresse::getPostnunmmer).ifPresent(visitingAddress::setPostnummer);
        address.map(Enhet.Adresse::getPoststed).ifPresent(visitingAddress::setPoststed);

        return visitingAddress;
    }
}
