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

    static public void registerAdjustedActivity(Map<String, AtomicLong> numberOfAdjustedActivitiesByType, Activity activity) {
        if (!numberOfAdjustedActivitiesByType.containsKey(activity.getType())) {
            numberOfAdjustedActivitiesByType.put(activity.getType(), new AtomicLong());
        }

        numberOfAdjustedActivitiesByType.get(activity.getType()).incrementAndGet();
    }

    public void adjustZeroDurationActivities(String populationSourcePath, String populationTargetPath) {
        Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationSourcePath);

        Counter counter = new Counter("", " persons checked");

        long numberOfPlans = 0;
        long numberOfAdjustedPlans = 0;

        long numberOfActivities = 0;
        long numberOfAdjustedActivities = 0;

        long numberOfAdjustedWrappedFirstActivities = 0;
        long numberOfAdjustedSingleFirstActivities = 0;
        long numberOfAdjustedSingleLastActivities = 0;

        Map<String, AtomicLong> numberOfAdjustedActivitiesByType = new HashMap<>();

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) { // TODO: check hopefully only one !?
                boolean isPlanAdjusted = false;

                Activity firstActivity = ((Activity) plan.getPlanElements().get(0));
                Activity lastActivity = ((Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1));

                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;
                        numberOfActivities++;

                        if (activity != firstActivity && activity != lastActivity) {
                            if (activity.getEndTime() - activity.getStartTime() <= 0.0) {
                                activity.setEndTime(activity.getStartTime() + ADJUSTMENT_OFFSET);

                                isPlanAdjusted = true;
                                numberOfAdjustedActivities++;

                                registerAdjustedActivity(numberOfAdjustedActivitiesByType, activity);
                            }
                        }
                    }
                }

                if (firstActivity != lastActivity) {
                    if (firstActivity.getType().equals(lastActivity.getType())) {
                        double wrappedStartTime = lastActivity.getStartTime() - 24.0 * 3600.0;
                        double duration = firstActivity.getEndTime() - wrappedStartTime;

                        if (duration <= 0.0) {
                            firstActivity.setEndTime(wrappedStartTime + ADJUSTMENT_OFFSET);
                            isPlanAdjusted = true;
                            numberOfAdjustedWrappedFirstActivities++;
                            registerAdjustedActivity(numberOfAdjustedActivitiesByType, firstActivity);
                        }
                    } else {
                        double firstDuration = firstActivity.getEndTime();
                        double lastDuration = lastActivity.getEndTime();

                        if (firstDuration <= 0.0) {
                            firstActivity.setEndTime(firstActivity.getStartTime() + ADJUSTMENT_OFFSET);
                            isPlanAdjusted = true;
                            numberOfAdjustedSingleFirstActivities++;
                            registerAdjustedActivity(numberOfAdjustedActivitiesByType, firstActivity);
                        }

                        if (lastDuration <= 0.0) {
                            lastActivity.setEndTime(lastActivity.getStartTime() + ADJUSTMENT_OFFSET);
                            isPlanAdjusted = true;
                            numberOfAdjustedSingleLastActivities++;
                            registerAdjustedActivity(numberOfAdjustedActivitiesByType, lastActivity);
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
        logger.info(String.format("Number of adjusted activities: %d (%.2f%%)", numberOfAdjustedActivities, 100.0 * (double)numberOfAdjustedActivities / (double)numberOfActivities));

        logger.info(String.format("Number of adjusted single first activities: %d (%.2f%%)", numberOfAdjustedSingleFirstActivities, 100.0 * (double)numberOfAdjustedSingleFirstActivities / (double)numberOfActivities));
        logger.info(String.format("Number of adjusted single last activities: %d (%.2f%%)", numberOfAdjustedSingleLastActivities, 100.0 * (double)numberOfAdjustedSingleLastActivities / (double)numberOfActivities));
        logger.info(String.format("Number of adjusted wrapped first activities: %d (%.2f%%)", numberOfAdjustedWrappedFirstActivities, 100.0 * (double)numberOfAdjustedWrappedFirstActivities / (double)numberOfActivities));

        logger.info("Number of adjusted activities by type:");

        for (Map.Entry<String, AtomicLong> entry : numberOfAdjustedActivitiesByType.entrySet()) {
            logger.info("    " + entry.getKey() + ": " + entry.getValue().get());
        }

        new PopulationWriter(scenario.getPopulation()).write(populationTargetPath);
    }
}
