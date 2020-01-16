package no.fint.kulturtanken.util;

import lombok.Data;
import org.springframework.security.core.Authentication;

@Data
public class CurrentRequest {
    private String orgId;
    private Authentication principal;
}
