package contrib.baseline.modification;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

/**
 * Converts plans where a car is used but NOT starting from the home location.
 * (Messes up SubtourModeChoice otherwise ...)
 */
public class ModeChainConsistency {
    static private Logger logger = Logger.getLogger(ModeChainConsistency.class);

    static public void main(String[] args) {
        String populationSourcePath = args[0];
        String populationTargetPath = args[1];
        new ModeChainConsistency().adjustInvalidModeChains(populationSourcePath, populationTargetPath);
    }

    public void adjustInvalidModeChains(String populationSourcePath, String populationTargetPath) {
        Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationSourcePath);

        Counter counter = new Counter("", " persons checked");

        long numberOfPlans = 0;
        long numberOfInvalidPlans = 0;
        long numberOfAdjustedLegs = 0;

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) { // TODO: check hopefully only one !?
                boolean startsWithCar = false;
                boolean usesCar = false;
                boolean isFirst = true;

                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Leg) {
                        Leg leg = (Leg) element;

                        if (leg.getMode().equals(TransportMode.car)) {
                            if (isFirst) {
                                startsWithCar = true;
                            }

                            usesCar = true;
                        }

                        isFirst = false;
                    }
                }

                numberOfPlans++;

                if (usesCar && !startsWithCar) {
                    numberOfInvalidPlans++;

                    for (PlanElement element : plan.getPlanElements()) {
                        if (element instanceof Leg) {
                            Leg leg = (Leg) element;

                            if (leg.getMode().equals(TransportMode.car)) {
                                leg.setMode(TransportMode.car);
                                numberOfAdjustedLegs++;
                            }
                        }
                    }
                }
            }

            counter.incCounter();
        }

        logger.info(String.format("Number of plans: %d", numberOfPlans));
        logger.info(String.format("Number of invalid plans: %d (%.2f%%)", numberOfInvalidPlans, 100.0 * (double)numberOfInvalidPlans / (double)numberOfPlans));
        logger.info(String.format("Number of adjusted legs: %d", numberOfAdjustedLegs));

        new PopulationWriter(scenario.getPopulation()).write(populationTargetPath);
    }
}
