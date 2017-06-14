package contrib.baseline.modification;

import contrib.baseline.preparation.IVTConfigCreator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * Adds those agents to a subpopulaton that have different first and last activity types
 */
public class DifferentFirstLastActivityTypes {
    static private Logger logger = Logger.getLogger(ModeChainConsistency.class);

    static public void main(String[] args) {
        String populationSourcePath = args[0];
        String populationAttributesSourcePath = args[1];
        String populationTargetPath = args[2];
        String populationAttributesTargetPath = args[03];
        new DifferentFirstLastActivityTypes().adjustPlans(populationSourcePath, populationAttributesSourcePath, populationTargetPath, populationAttributesTargetPath);
    }

    public void adjustPlans(String populationSourcePath, String populationAttributesSourcePath, String populationTargetPath, String populationAttributesTargetPath) {
        Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationSourcePath);
        new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(populationAttributesSourcePath);

        Counter counter = new Counter("", " persons checked");

        long numberOfPlans = 0;
        long numberOfAdjustedPlans = 0;

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) { // TODO: check hopefully only one !?
                String firstActivityType = ((Activity) plan.getPlanElements().get(0)).getType();
                String lastActivityType = ((Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1)).getType();

                numberOfPlans++;

                if (!firstActivityType.equals(lastActivityType)) {
                    numberOfAdjustedPlans++;
                    scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", IVTConfigCreator.DIFF_FIRST_LAST_TAG);
                }
            }

            counter.incCounter();
        }

        logger.info(String.format("Number of plans: %d", numberOfPlans));
        logger.info(String.format("Number of invalid plans: %d (%.2f%%)", numberOfAdjustedPlans, 100.0 * (double)numberOfAdjustedPlans / (double)numberOfPlans));

        new PopulationWriter(scenario.getPopulation()).write(populationTargetPath);
        new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(populationAttributesTargetPath);
    }
}
