package no.fint.kulturtanken

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon
import no.fint.model.resource.Link
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources
import no.fint.model.resource.felles.kompleksedatatyper.AdresseResource
import no.fint.model.resource.utdanning.elev.BasisgruppeResource
import no.fint.model.resource.utdanning.elev.BasisgruppeResources
import no.fint.model.resource.utdanning.timeplan.FagResource
import no.fint.model.resource.utdanning.timeplan.FagResources
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResources
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources

class FintObjectFactory {

    static SkoleResources newSchoolResources() {
        SkoleResources resources = new SkoleResources()
        resources.addResource(newSchool())
        return resources
    }

    static SkoleResource newSchool() {
        SkoleResource resource = new SkoleResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'HVS@11011'))
        resource.setNavn('Haugaland videregående skole')
        resource.setOrganisasjonsnummer(new Identifikator(identifikatorverdi: '974624486'))
        resource.setSkolenummer(new Identifikator(identifikatorverdi: '11011'))
        resource.setKontaktinformasjon(new Kontaktinformasjon(epostadresse: 'haugaland-vgs@skole.rogfk.no', telefonnummer: '52 86 56 00'))
        resource.setPostadresse(new AdresseResource(adresselinje: Arrays.asList('Spannavegen 38'), postnummer: '5531', poststed: 'Haugesund' ))
        resource.addSelf(new Link(verdi: 'https://hvs'))
        return resource
    }

    static ArstrinnResources newLevelResources() {
        ArstrinnResources resources = new ArstrinnResources()
        resources.addResource(newLevel())
        return resources
    }

    static ArstrinnResource newLevel() {
        ArstrinnResource resource = new ArstrinnResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'vg1'))
        resource.setPeriode(Collections.emptyList())
        resource.setNavn('vg1')
        resource.setBeskrivelse('Videregående trinn 1')
        resource.addSelf(new Link(verdi: 'https://vg1'))
        return resource
    }

    static BasisgruppeResources newBasisGroupResources() {
        BasisgruppeResources resources = new BasisgruppeResources()
        resources.addResource(newBasisGroup())
        return resources
    }

    static BasisgruppeResource newBasisGroup() {
        BasisgruppeResource resource = new BasisgruppeResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: '1TIA_HVS@11011'))
        resource.setPeriode(Collections.emptyList())
        resource.setNavn('1TIA_HVS')
        resource.setBeskrivelse('Basisgruppe 1TIA ved Haugaland videregående skole')
        resource.addSkole(new Link(verdi: 'https://hvs'))
        resource.addTrinn(new Link(verdi: 'https://vg1'))
        resource.addElevforhold(new Link(verdi: 'https://elev01'))
        return resource
    }

    static FagResources newSubjectResources() {
        FagResources resources = new FagResources()
        resources.addResource(newSubject())
        return resources
    }

    static FagResource newSubject() {
        FagResource resource = new FagResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'YFF4106'))
        resource.setPeriode(Collections.emptyList())
        resource.setNavn('YFF4106')
        resource.setBeskrivelse('Yrkesfaglig fordypning Vg1')
        resource.addSelf(new Link(verdi: 'https://yff4106'))
        return resource
    }

    static UndervisningsgruppeResources newTeachingGroupResources() {
        UndervisningsgruppeResources resources = new UndervisningsgruppeResources()
        resources.addResource(newTeachingGroup())
        return resources
    }

    static UndervisningsgruppeResource newTeachingGroup() {
        UndervisningsgruppeResource resource = new UndervisningsgruppeResource()
        resource.setSystemId(new Identifikator(identifikatorverdi: 'YFF4106_HVS@11011'))
        resource.setPeriode(Collections.emptyList())
        resource.setNavn('YFF4106_HVS')
        resource.setBeskrivelse('Undervisningsgruppe YFF4106 ved Haugaland videregående skole')
        resource.addSkole(new Link(verdi: 'https://hvs'))
        resource.addFag(new Link(verdi: 'https://yff4106'))
        resource.addElevforhold(new Link(verdi: 'https://elev01'))
        return resource
    }
}
