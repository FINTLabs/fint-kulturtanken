package no.fint.kulturtanken.util

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon
import no.fint.model.resource.Link
import no.fint.model.resource.felles.kompleksedatatyper.AdresseResource
import no.fint.model.resource.utdanning.elev.BasisgruppeResource
import no.fint.model.resource.utdanning.timeplan.FagResource
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource

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
        resource.addBasisgruppe(new Link(verdi: 'link.To.BasisGroup'))
        resource.addUndervisningsgruppe(new Link(verdi: 'link.To.TeachingGroup'))
        resource.addSelf(new Link(verdi: 'link.To.School'))
        return resource
    }

    static ArstrinnResource newLevel() {
        ArstrinnResource resource = new ArstrinnResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'l'))
        resource.setPeriode(Collections.emptyList())
        resource.setNavn('Level')
        resource.setBeskrivelse('A level')
        resource.addSelf(new Link(verdi: 'link.To.Level'))
        return resource
    }

    static BasisgruppeResource newBasisGroup() {
        BasisgruppeResource resource = new BasisgruppeResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'BG'))
        resource.setPeriode(Collections.emptyList())
        resource.setNavn('Basis group')
        resource.setBeskrivelse('Basis group at school')
        resource.addSkole(new Link(verdi: 'link.To.School'))
        resource.addTrinn(new Link(verdi: 'link.To.Level'))
        resource.addElevforhold(new Link(verdi: 'link.To.StudentRelation'))
        resource.addSelf(new Link(verdi: 'link.To.BasisGroup'))
        return resource
    }

    static FagResource newSubject() {
        FagResource resource = new FagResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'S'))
        resource.setPeriode(Collections.emptyList())
        resource.setNavn('Subject')
        resource.setBeskrivelse('A subject')
        resource.addSelf(new Link(verdi: 'link.To.Subject'))
        return resource
    }

    static UndervisningsgruppeResource newTeachingGroup() {
        UndervisningsgruppeResource resource = new UndervisningsgruppeResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'TG'))
        resource.setPeriode(Collections.emptyList())
        resource.setNavn('Teaching group')
        resource.setBeskrivelse('Teaching group at school')
        resource.addSkole(new Link(verdi: 'link.To.School'))
        resource.addFag(new Link(verdi: 'link.To.Subject'))
        resource.addElevforhold(new Link(verdi: 'link.To.StudentRelation'))
        resource.addSelf(new Link(verdi: 'link.To.TeachingGroup'))
        return resource
    }
}
