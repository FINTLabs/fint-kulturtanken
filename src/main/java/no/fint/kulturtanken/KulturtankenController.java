package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Skoleeier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/skoleeier")
public class KulturtankenController {
    private final KulturtankenService kulturtankenService;
    private final KulturtankenProperties kulturtankenProperties;

    public KulturtankenController(KulturtankenService kulturtankenService, KulturtankenProperties kulturtankenProperties) {
        this.kulturtankenService = kulturtankenService;
        this.kulturtankenProperties = kulturtankenProperties;
    }

    @GetMapping("/{orgId}")
    public Skoleeier getSchoolOwner(@PathVariable String orgId) {
        log.info(orgId);
        if (kulturtankenProperties.getOrganisations().containsKey(orgId)) {
            return kulturtankenService.getSchoolOwner(orgId);
        } else {
            throw new exceptionHandler.SchoolOwnerNotFoundException(String.format("School owner not found for organisation number: %s", orgId));
        }
    }

    @GetMapping
    public Stream<KulturtankenProperties.Organisation> getOrganisations() {
        return kulturtankenProperties.getOrganisations().entrySet()
                .stream()
                .map(organisation -> {
                    organisation.getValue().setUri(ServletUriComponentsBuilder.fromCurrentContextPath()
                            .pathSegment("skoleeier", organisation.getKey()).build().toUri());
                    return organisation.getValue();
                }).sorted(Comparator.comparing(KulturtankenProperties.Organisation::getName));
    }
}
