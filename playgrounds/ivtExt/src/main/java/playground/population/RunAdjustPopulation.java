package playground.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.Random;

public class RunAdjustPopulation {
    static public String CAR_OWNERSHIP = "car_ownership";
    static public Long RANDOM_SEED = 0L;

    static private boolean isValidPerson(Person person) {
        return !(person.getId().toString().startsWith("cb_") || person.getId().toString().startsWith("freight_"));
    }

    static private boolean isCarOwner(Person person) {
        String carAvailability = (String) person.getAttributes().getAttribute("car_avail");
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
        String populationInputPath = args[0];
        String populationAttributesInputPath = args[1];

        AdjustPopulationConfigGroup adjustmentConfig = new AdjustPopulationConfigGroup();
        Config config = ConfigUtils.createConfig(adjustmentConfig);

        config.plans().setInputFile(populationInputPath);
        config.plans().setInputPersonAttributeFile(populationAttributesInputPath.equals("null") ? null : populationAttributesInputPath);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Population population = scenario.getPopulation();
        ObjectAttributes attributes = population.getPersonAttributes();

        Double carOwnershipRate = adjustmentConfig.getCarOwnershipRate();
        double removeCarOwnershipRate = 0.0;

        if (carOwnershipRate == null) {
            logger.info("Not adjusting car ownership");
        } else {
            double initialCarOwnershipRate = getCarAvailability(population);

            logger.info("Initial car ownership rate: " + initialCarOwnershipRate);
            logger.info(String.format("Adjusted car ownership rate: %.2f%%", carOwnershipRate));
            removeCarOwnershipRate = 1.0 - carOwnershipRate / initialCarOwnershipRate;

            logger.info(String.format("Removing %.2f%% of current car ownership flags", removeCarOwnershipRate));
        }

        Double evOwnershipRate = adjustmentConfig.getEvOwnershipRate();

        if (evOwnershipRate == null) {
            logger.info("Not setting any EV ownership.");
        } else {
            logger.info(String.format("Setting EV ownership to %.2f%%", evOwnershipRate));
        }

        Double homeOfficeRate = adjustmentConfig.getHomeOfficeRate();

        if (homeOfficeRate == null) {
            logger.info("Not setting any home office.");
        } else {
            logger.info(String.format("Setting home office to %.2f%%", homeOfficeRate));
        }

        Random random = RANDOM_SEED == null ? new Random() : new Random(RANDOM_SEED);
        for (int i = 0; i < 1000000; i++) random.nextDouble();

        for (Person person : population.getPersons().values()) {
            if (!isValidPerson(person)) continue;

            if (evOwnershipRate != null && random.nextDouble() < evOwnershipRate) {
                attributes.putAttribute(person.getId().toString(), "ev", true);
            }

            if (carOwnershipRate != null && isCarOwner(person) && random.nextDouble() < removeCarOwnershipRate) {
                person.getAttributes().putAttribute(CAR_OWNERSHIP, "never");
            }

            // TODO: Missing Home Office
        }
    }
}

