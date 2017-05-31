package playground.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.omg.PortableInterceptor.ACTIVE;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class RunAdjustPopulation {
    static public String CAR_OWNERSHIP = "carAvail";
    static public Long RANDOM_SEED = 0L;

    static private boolean isValidPerson(Person person) {
        return !(person.getId().toString().startsWith("cb_") || person.getId().toString().startsWith("freight_"));
    }

    static private boolean isCarOwner(Person person) {
        String carAvailability = (String) person.getAttributes().getAttribute(CAR_OWNERSHIP);
        return carAvailability != null && (carAvailability.equals("always") || carAvailability.equals("sometimes"));
    }

    static private double getCarAvailability(Population population) {
        double numberOfPersons = 0.0;
        double numberOfCarOwners = 0.0;

        logger.info("Calculating car ownership ...");
        Counter counter = new Counter("Counted ", " persons");

        for (Person person : population.getPersons().values()) {
            counter.incCounter();
            if (!isValidPerson(person)) continue;

            if (isCarOwner(person)) numberOfCarOwners += 1.0;
            numberOfPersons += 1.0;
        }

        return numberOfCarOwners / numberOfPersons;
    }

    static public Logger logger = Logger.getLogger(RunAdjustPopulation.class);

    static public void main(String[] args) {
        String configPath = args[0];
        String populationOutputPath = args[1];
        String populationAttributesOutputPath = args[2];

        AdjustPopulationConfigGroup adjustmentConfig = new AdjustPopulationConfigGroup();
        Config config = ConfigUtils.loadConfig(configPath, adjustmentConfig);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Population population = scenario.getPopulation();
        ObjectAttributes attributes = population.getPersonAttributes();

        Double carOwnershipRate = adjustmentConfig.getCarOwnershipRate();
        double removeCarOwnershipRate = 0.0;

        if (carOwnershipRate == null) {
            logger.info("Not adjusting car ownership");
        } else {
            double initialCarOwnershipRate = getCarAvailability(population);

            logger.info(String.format("Initial car ownership rate: %.2f%%", initialCarOwnershipRate * 100));
            logger.info(String.format("Adjusted car ownership rate: %.2f%%", carOwnershipRate * 100));
            removeCarOwnershipRate = 1.0 - carOwnershipRate / initialCarOwnershipRate;

            logger.info(String.format("Removing %.2f%% of current car ownership flags", removeCarOwnershipRate * 100));
        }

        Double evOwnershipRate = adjustmentConfig.getEvOwnershipRate();

        if (evOwnershipRate == null) {
            logger.info("Not setting any EV ownership.");
        } else {
            logger.info(String.format("Setting EV ownership to %.2f%%", evOwnershipRate * 100));
        }

        Double ebikeOwnershipRate = adjustmentConfig.getEbikeOwnershipRate();

        if (ebikeOwnershipRate == null) {
            logger.info("Not setting any ebike ownership.");
        } else {
            logger.info(String.format("Setting ebike ownership to %.2f%%", ebikeOwnershipRate * 100));
        }

        Double homeOfficeRate = adjustmentConfig.getHomeOfficeRate();

        if (homeOfficeRate == null) {
            logger.info("Not setting any home office.");
        } else {
            logger.info(String.format("Setting home office to %.2f%%", homeOfficeRate * 100));
        }

        Random random = RANDOM_SEED == null ? new Random() : new Random(RANDOM_SEED);
        for (int i = 0; i < 1000000; i++) random.nextDouble();

        Counter counter = new Counter("Processed ", " persons");
        for (Person person : population.getPersons().values()) {
            counter.incCounter();
            if (!isValidPerson(person)) continue;

            if (evOwnershipRate != null && random.nextDouble() < evOwnershipRate) {
                attributes.putAttribute(person.getId().toString(), "ev", true);
            }

            if (ebikeOwnershipRate != null && random.nextDouble() < evOwnershipRate) {
                attributes.putAttribute(person.getId().toString(), "ebike", true);
            }

            if (carOwnershipRate != null && isCarOwner(person) && random.nextDouble() < removeCarOwnershipRate) {
                person.getAttributes().putAttribute(CAR_OWNERSHIP, "never");
            }

            if (homeOfficeRate != null) {
                Plan plan = person.getSelectedPlan();
                Activity homeActivity = null;

                List<PlanElement> elements = plan.getPlanElements();

                for (PlanElement element : elements) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;

                        if (activity.getType().equals("home")) {
                            homeActivity = activity;
                        } else if (activity.getType().equals("work") && homeActivity != null && random.nextDouble() < homeOfficeRate) {
                            activity.setType(homeActivity.getType());
                            activity.setCoord(homeActivity.getCoord());
                            activity.setLinkId(homeActivity.getLinkId());
                            activity.setFacilityId(homeActivity.getFacilityId());
                        }
                    }
                }

                if (homeActivity != null) {
                    Activity previousActivity = null;
                    Leg previousLeg = null;

                    List<Leg> legsForRemoval = new LinkedList<>();

                    for (PlanElement element : elements) {
                        if (element instanceof Activity) {
                            Activity activity = (Activity) element;

                            if (previousActivity != null && previousActivity.getType().equals("home") && activity.getType().equals("home")) {
                                legsForRemoval.add(previousLeg);
                            }

                            previousActivity = activity;
                        } else if (element instanceof Leg) {
                            previousLeg = (Leg) element;
                        }
                    }

                    for (Leg leg : legsForRemoval) {
                        elements.remove(leg);
                    }

                    Iterator<PlanElement> iterator = elements.iterator();
                    Activity activeHomeActivity = null;

                    while (iterator.hasNext()) {
                        PlanElement element = iterator.next();

                        if (element instanceof Activity) {
                            Activity activity = (Activity) element;

                            if (activity.getType().equals("home") && activeHomeActivity != null) {
                                activeHomeActivity.setEndTime(activity.getEndTime());
                                iterator.remove();
                            } else {
                                activeHomeActivity = null;
                            }
                        }
                    }
                }
            }
        }

        new PopulationWriter(population).write(populationOutputPath);
        new ObjectAttributesXmlWriter(attributes).writeFile(populationAttributesOutputPath);
    }
}

