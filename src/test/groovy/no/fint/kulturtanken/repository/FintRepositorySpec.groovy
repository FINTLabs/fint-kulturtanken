package no.fint.kulturtanken.repository

import no.fint.kulturtanken.util.FintObjectFactory
import no.fint.model.resource.Link
import no.fint.model.resource.utdanning.elev.BasisgruppeResources
import no.fint.model.resource.utdanning.timeplan.FagResources
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResources
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class FintRepositorySpec extends Specification {
    private FintRepository fintRepository
    private RestTemplate restTemplate;

    void setup() {
        restTemplate = Mock();
        fintRepository = new FintRepository(restTemplate)
    }

    def "Get schools from Fint"() {
        given:
        def school = FintObjectFactory.newSchool()
        def schools = new SkoleResources()
        schools.addResource(school)

        when:
        def resources = fintRepository.getSchools(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<SkoleResources>) >> schools
        resources.getTotalItems() == 1
        resources.getContent().get(0).navn == 'School'
    }

    def "Get basis groups from Fint"() {
        given:
        def basisGroup = FintObjectFactory.newBasisGroup()
        def basisGroups = new BasisgruppeResources()
        basisGroups.addResource(basisGroup)

        when:
        def resources = fintRepository.getBasisGroups(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<BasisgruppeResources>) >> basisGroups
        resources.size() == 1
        resources.get(Link.with('link.To.BasisGroup')).navn == 'Basis group'
    }

    def "Get levels from Fint"() {
        given:
        def level = FintObjectFactory.newLevel()
        def levels = new ArstrinnResources()
        levels.addResource(level)

        when:
        def resources = fintRepository.getLevels(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<ArstrinnResources>) >> levels
        resources.size() == 1
        resources.get(Link.with('link.To.Level')).navn == 'Level'
    }

    def "Get teaching groups from Fint"() {
        given:
        def teachingGroup = FintObjectFactory.newTeachingGroup()
        def teachingGroups = new UndervisningsgruppeResources()
        teachingGroups.addResource(teachingGroup)

        when:
        def resources = fintRepository.getTeachingGroups(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<UndervisningsgruppeResources>) >> teachingGroups
        resources.size() == 1
        resources.get(Link.with('link.To.TeachingGroup')).navn == 'Teaching group'
    }

    def "Get subjects from Fint"() {
        given:
        def subject = FintObjectFactory.newSubject()
        def subjects = new FagResources()
        subjects.addResource(subject)

        when:
        def resources = fintRepository.getSubjects(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<FagResources>) >> subjects
        resources.size() == 1
        resources.get(Link.with('link.To.Subject')).navn == 'Subject'
    }
}
