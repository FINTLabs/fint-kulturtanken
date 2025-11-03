package no.fint.kulturtanken.util

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.Link
import no.fint.model.resource.felles.kompleksedatatyper.AdresseResource
import no.fint.model.resource.utdanning.elev.KlasseResource
import no.fint.model.resource.utdanning.kodeverk.SkolearResource
import no.fint.model.resource.utdanning.timeplan.FagResource
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource

import java.text.SimpleDateFormat

class FintObjectFactory {

    static SkoleResource newSchool() {
        SkoleResource resource = new SkoleResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'S'))
        resource.setNavn('School')
        resource.setOrganisasjonsnummer(new Identifikator(identifikatorverdi: '012345678'))
        resource.setSkolenummer(new Identifikator(identifikatorverdi: '1111'))
        resource.setKontaktinformasjon(new Kontaktinformasjon(epostadresse: 'school@schools.no', telefonnummer: '00 11 22 33'))
        resource.setForretningsadresse(new AdresseResource(adresselinje: Arrays.asList('Address'), postnummer: '0123', poststed: 'City' ))
        resource.setPostadresse(new AdresseResource(adresselinje: Arrays.asList('Address'), postnummer: '0123', poststed: 'City' ))
        resource.addKlasse(new Link(verdi: 'link.To.Klasse'))
        resource.addUndervisningsgruppe(new Link(verdi: 'link.To.TeachingGroup'))
        resource.addSelf(new Link(verdi: 'link.To.School'))
        return resource
    }

    static ArstrinnResource newLevel() {
        ArstrinnResource resource = new ArstrinnResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'l'))
        resource.setNavn('Level')
        resource.setBeskrivelse('A level')
        resource.addGrepreferanse(new Link(verdi: 'link.To/Grep'))
        resource.addSelf(new Link(verdi: 'link.To.Level'))
        return resource
    }

    static KlasseResource newKlasse() {
        KlasseResource resource = new KlasseResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'BG'))
        resource.setNavn('Klasse')
        resource.setBeskrivelse('Klasse at school')
        resource.addSkole(new Link(verdi: 'link.To.School'))
        resource.addTrinn(new Link(verdi: 'link.To.Level'))
        resource.addKlassemedlemskap(new Link(verdi: 'link.To.StudentRelation'))
        resource.addSelf(new Link(verdi: 'link.To.Klasse'))
        return resource
    }

    static FagResource newSubject() {
        FagResource resource = new FagResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'S'))
        resource.setNavn('Subject')
        resource.setBeskrivelse('A subject')
        resource.addGrepreferanse(new Link(verdi: 'link.To/Grep'))
        resource.addSelf(new Link(verdi: 'link.To.Subject'))
        return resource
    }

    static UndervisningsgruppeResource newTeachingGroup() {
        UndervisningsgruppeResource resource = new UndervisningsgruppeResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'TG'))
        resource.setNavn('Teaching group')
        resource.setBeskrivelse('Teaching group at school')
        resource.addSkole(new Link(verdi: 'link.To.School'))
        resource.addFag(new Link(verdi: 'link.To.Subject'))
        resource.addGruppemedlemskap(new Link(verdi: 'link.To.StudentRelation'))
        resource.addSelf(new Link(verdi: 'link.To.TeachingGroup'))
        return resource
    }

    static SkolearResource newSchoolYear() {
        SkolearResource resource = new SkolearResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'SY'))
        resource.setNavn('School Year')
        resource.setGyldighetsperiode(newPeriode())
        resource.setKode("SchoolYearCode")
        resource.addSelf(new Link(verdi: 'link.To.SchoolYear'))
        return resource
    }

    static Periode newPeriode() {
        Periode resource = new Periode()
        resource.setBeskrivelse("period description")
        def sdf = new SimpleDateFormat("yyyy-MM-dd")
        resource.setStart(sdf.parse("2025-08-15"))
        resource.setSlutt(sdf.parse("2026-06-15"))
        return resource
    }
}
