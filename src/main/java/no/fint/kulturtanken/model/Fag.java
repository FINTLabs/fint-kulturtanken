package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class Fag {
    private String fagkode;
    private List<Undervisningsgruppe> undervisningsgrupper;
}
