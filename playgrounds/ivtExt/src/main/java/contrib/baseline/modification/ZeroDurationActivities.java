package contrib.baseline.modification;

import contrib.baseline.preparation.IVTConfigCreator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ZeroDurationActivities {
    static private Logger logger = Logger.getLogger(ZeroDurationActivities.class);

    static public void main(String[] args) {
        String populationSourcePath = args[0];
        String populationTargetPath = args[1];
        new ZeroDurationActivities().adjustZeroDurationActivities(populationSourcePath, populationTargetPath);
    }

    static public double ADJUSTMENT_OFFSET = 60.0;

    public void adjustZeroDurationActivities(String populationSourcePath, String populationTargetPath) {
        Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationSourcePath);

        Counter counter = new Counter("", " persons checked");

        long numberOfPlans = 0;
        long numberOfAdjustedPlans = 0;

        long numberOfActivities = 0;
        long numberOfAdjustedActivities = 0;

        Map<String, AtomicLong> numberOfAdjustedActivitiesByType = new HashMap<>();

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) { // TODO: check hopefully only one !?
                boolean isPlanAdjusted = false;

                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;
                        numberOfActivities++;

                        if (activity.getStartTime() != Time.UNDEFINED_TIME && activity.getEndTime() != Time.UNDEFINED_TIME) {
                            if (activity.getEndTime() - activity.getStartTime() <= 0.0) {
                                activity.setEndTime(activity.getEndTime() + ADJUSTMENT_OFFSET);

                                isPlanAdjusted = true;
                                numberOfAdjustedActivities++;

                                if (!numberOfAdjustedActivitiesByType.containsKey(activity.getType())) {
                                    numberOfAdjustedActivitiesByType.put(activity.getType(), new AtomicLong());
                                }

                                numberOfAdjustedActivitiesByType.get(activity.getType()).incrementAndGet();
                            }
                        }
                    }
                }

                numberOfPlans++;
                if (isPlanAdjusted) numberOfAdjustedPlans++;
            }

            counter.incCounter();
        }

        logger.info(String.format("Number of plans: %d", numberOfPlans));
        logger.info(String.format("Number of adjusted plans: %d (%.2f%%)", numberOfAdjustedPlans, 100.0 * (double)numberOfAdjustedPlans / (double)numberOfPlans));
        logger.info(String.format("Number of activities: %d", numberOfActivities));
        logger.info(String.format("Number of adjusted activities: %d (%.2f%%)", numberOfAdjustedPlans, 100.0 * (double)numberOfAdjustedActivities / (double)numberOfActivities));

        logger.info("Number of adjusted activities by type:");

        for (Map.Entry<String, AtomicLong> entry : numberOfAdjustedActivitiesByType.entrySet()) {
            logger.info("    " + entry.getKey() + ": " + entry.getValue().get());
        }

        new PopulationWriter(scenario.getPopulation()).write(populationTargetPath);
    }
}
