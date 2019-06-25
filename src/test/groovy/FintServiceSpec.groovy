import no.fint.kulturtanken.FintServiceTestComponents
import spock.lang.Specification

class FintServiceSpec extends Specification {
    def fintserviceTestComponents = new FintServiceTestComponents()
    def "Get skoleOrganisasjonTestData check school size is four"() {
        given:

        when:
        def skoleOrganisasjon = fintserviceTestComponents.skoleOrganisasjonTestData

        then:
        skoleOrganisasjon.skole.size() == 2
    }
    def "Get skoleOrganisasjonTestData check school level size is three and zero"() {
        given:

        when:
        def skoleOrganisasjon = fintserviceTestComponents.skoleOrganisasjonTestData

        then:
        skoleOrganisasjon.skole[0].trinn.size() == 3
        skoleOrganisasjon.skole[1].trinn.size() == 0
    }

    def "Get skoleOrganisasjonTestData check if school basisgruppe has correct amount"() {
        given:

        when:
        def skoleOrganisasjon = fintserviceTestComponents.skoleOrganisasjonTestData

        then:
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper.size() == 2
        skoleOrganisasjon.skole[0].trinn[1].basisgrupper.size() == 1
        skoleOrganisasjon.skole[0].trinn[2].basisgrupper.size() == 1
    }
    def "Get skoleOrganisasjonTestData check if school basisgruppe has students"() {
        given:

        when:
        def skoleOrganisasjon = fintserviceTestComponents.skoleOrganisasjonTestData

        then:
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper.each {
            it.antall>0
        }
    }

}