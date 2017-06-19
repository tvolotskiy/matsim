package playground.mas.scenario;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.mas.MASConfigGroup;
import playground.mas.MASModule;
import playground.mas.zurich.ZurichMASConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import playground.sebhoerl.avtaxi.routing.AVRouteFactory;

import java.util.*;

public class RunBuildScenario {
    static private Long RANDOM_SEED = 1L;
    static public String CAR_OWNERSHIP = "carAvail";
    static public String HAS_LICENSE = "hasLicense";

    static public void main(String[] args) {
        MASConfigGroup masConfigGroup = new MASConfigGroup();
        MASPopulationConfigGroup masPopulationConfigGroup = new MASPopulationConfigGroup();
        ZurichMASConfigGroup zurichMASConfigGroup = new ZurichMASConfigGroup();

        Config config = ConfigUtils.loadConfig(args[0], masConfigGroup, masPopulationConfigGroup, zurichMASConfigGroup);

        String scenarioPopulationPath = ConfigGroup.getInputFileURL(config.getContext(), config.plans().getInputFile()).getPath();
        String networkPopulationPath = ConfigGroup.getInputFileURL(config.getContext(), config.network().getInputFile()).getPath();

        config.plans().setInputFile(ConfigGroup.getInputFileURL(config.getContext(), masPopulationConfigGroup.getOriginalPopulationPath()).getPath());
        config.network().setInputFile(ConfigGroup.getInputFileURL(config.getContext(), masPopulationConfigGroup.getOriginalNetworkPath()).getPath());

        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(AVRoute.class, new AVRouteFactory());
        ScenarioUtils.loadScenario(scenario);

        RunBuildScenario builder = new RunBuildScenario(scenario, masConfigGroup, masPopulationConfigGroup, zurichMASConfigGroup);
        builder.run();

        builder.write(scenarioPopulationPath, networkPopulationPath);
    }

    static private Logger logger = Logger.getLogger(RunBuildScenario.class);

    final private Scenario scenario;
    final private MASConfigGroup masConfig;
    final private MASPopulationConfigGroup populationConfig;
    final private ZurichMASConfigGroup zurichConfig;
    final private Random random;

    public RunBuildScenario(Scenario scenario, MASConfigGroup masConfig, MASPopulationConfigGroup populationConfig, ZurichMASConfigGroup zurichConfig) {
        this.scenario = scenario;
        this.masConfig = masConfig;
        this.populationConfig = populationConfig;
        this.zurichConfig = zurichConfig;

        random = RANDOM_SEED == null ? new Random() : new Random(RANDOM_SEED);
        for (int i = 0; i < 1000000; i++) random.nextDouble();
    }

    private void run() {
        removeUnselectedPlans();
        applyEbikeOwnership();
        applyEVOwnership();
        adjustCarOwnership();
        applyHomeActivities("work", populationConfig.getHomeOfficeRate());
        applyHomeActivities("shop", populationConfig.getShoppingReductionRate());
        applyCordons();
        applyAVLinks();
    }

    private void write(String populationOutputPath, String networkOutputPath) {
        new PopulationWriter(scenario.getPopulation()).write(populationOutputPath);
        new NetworkWriter(scenario.getNetwork()).write(networkOutputPath);
    }

    private void removeUnselectedPlans() {
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            person.getPlans().clear();
            person.addPlan(selectedPlan);
        }
    }

    private void applyAVLinks() {
        Network network = scenario.getNetwork();

        logger.info("Assigning AV mode to links ...");
        long avLinks = 0;

        Id<Node> avAreaCenterNodeId = zurichConfig.getAVAreaCenterNodeId();
        Node avAreaCenterNode = avAreaCenterNodeId == null ? null : network.getNodes().get(avAreaCenterNodeId);
        double avAreaRadius = zurichConfig.getAVAreaRadius();

        for (Link link : network.getLinks().values()) {
            if (link.getAllowedModes().contains(TransportMode.car) &&
                    (avAreaCenterNode == null || CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), avAreaCenterNode.getCoord()) < avAreaRadius))
            {
                HashSet<String> allowedModes = new HashSet<>(link.getAllowedModes());
                allowedModes.add(AVModule.AV_MODE);
                link.setAllowedModes(allowedModes);
                avLinks++;
            }
        }

        logger.info("Found " + avLinks + " AV links");
    }

    private void applyCordons() {
        long innerCordonLinks = 0;
        long outerCordonLinks = 0;

        logger.info("Assigning cordon flags to links");

        Network network = scenario.getNetwork();

        Node innerCordonCenterNode = network.getNodes().get(masConfig.getInnerCordonCenterNodeId());
        double innerCordonRadius = masConfig.getInnerCordonRadius();

        for (Link link : scenario.getNetwork().getLinks().values()) {
            if (CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), innerCordonCenterNode.getCoord()) < innerCordonRadius) {
                link.getAttributes().putAttribute(MASModule.INNER_CORDON, true);
                innerCordonLinks++;
            }
        }

        Node outerCordonCenterNode = network.getNodes().get(masConfig.getOuterCordonCenterNodeId());
        double outerCordonRadius = masConfig.getOuterCordonRadius();

        for (Link link : scenario.getNetwork().getLinks().values()) {
            if (CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), outerCordonCenterNode.getCoord()) >= outerCordonRadius &&
                    CoordUtils.calcEuclideanDistance(link.getToNode().getCoord(), outerCordonCenterNode.getCoord()) <= outerCordonRadius) {
                link.getAttributes().putAttribute(MASModule.OUTER_CORDON, true);
                outerCordonLinks++;
            }
        }

        logger.info("Found " + innerCordonLinks + " for inner cordon");
        logger.info("Found " + outerCordonLinks + " for outer cordon");
    }

    private void applyHomeActivities(String activityType, Double rate) {
        if (rate == null) {
            logger.info(String.format("Not reducing any %s acitivites to home", activityType));
        } else {
            logger.info(String.format("Setting %.2f%% of %s to home", rate * 100, activityType));

            for (Person person : scenario.getPopulation().getPersons().values()) {
                if (!isValidPerson(person)) continue;

                Plan plan = person.getSelectedPlan();
                Activity homeActivity = null;

                List<PlanElement> elements = plan.getPlanElements();
                boolean activityChainChanged = false;

                for (PlanElement element : elements) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;

                        if (activity.getType().equals("home")) {
                            homeActivity = activity;
                        } else if (activity.getType().equals(activityType) && homeActivity != null && random.nextDouble() < rate) {
                            activity.setType(homeActivity.getType());
                            activity.setCoord(homeActivity.getCoord());
                            activity.setLinkId(homeActivity.getLinkId());
                            activity.setFacilityId(homeActivity.getFacilityId());
                            activityChainChanged = true;
                        }
                    }
                }

                if (homeActivity != null && activityChainChanged) {
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

                            if (activity.getType().equals("home")) {
                                if (activeHomeActivity == null) {
                                    activeHomeActivity = activity;
                                } else {
                                    activeHomeActivity.setEndTime(activity.getEndTime());
                                    iterator.remove();
                                }
                            } else {
                                activeHomeActivity = null;
                            }
                        } else if (element instanceof Leg) {
                            activeHomeActivity = null;

                            Leg leg = (Leg) element;

                            if (!leg.getMode().equals("av")) {
                                leg.setRoute(null);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isValidPerson(Person person) {
        return !(person.getId().toString().startsWith("cb_") || person.getId().toString().startsWith("freight_"));
    }

    private boolean isCarOwner(Person person) {
        String carAvailability = (String) person.getAttributes().getAttribute(CAR_OWNERSHIP);
        return carAvailability != null && (carAvailability.equals("always") || carAvailability.equals("sometimes"));
    }

    private double getCarAvailability(Population population) {
        double numberOfPersons = 0.0;
        double numberOfCarOwners = 0.0;

        logger.info("Calculating car ownership ...");

        for (Person person : population.getPersons().values()) {
            if (!isValidPerson(person)) continue;

            if (isCarOwner(person)) numberOfCarOwners += 1.0;
            numberOfPersons += 1.0;
        }

        return numberOfCarOwners / numberOfPersons;
    }

    private void adjustCarOwnership() {
        Double carOwnershipRate = populationConfig.getCarOwnershipRate();
        double removeCarOwnershipRate = 0.0;

        if (carOwnershipRate == null) {
            logger.info("Not adjusting car ownership");
        } else {
            double initialCarOwnershipRate = getCarAvailability(scenario.getPopulation());

            logger.info(String.format("Initial car ownership rate: %.2f%%", initialCarOwnershipRate * 100));
            logger.info(String.format("Adjusted car ownership rate: %.2f%%", carOwnershipRate * 100));
            removeCarOwnershipRate = 1.0 - carOwnershipRate / initialCarOwnershipRate;

            logger.info(String.format("Removing %.2f%% of current car ownership flags", removeCarOwnershipRate * 100));

            for (Person person : scenario.getPopulation().getPersons().values()) {
                if (!isValidPerson(person)) continue;

                if (isCarOwner(person) && random.nextDouble() < removeCarOwnershipRate) {
                    person.getAttributes().putAttribute(CAR_OWNERSHIP, "never");
                    person.getAttributes().putAttribute(HAS_LICENSE, "no");

                    for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                        if (element instanceof Leg) {
                            Leg leg = (Leg) element;

                            if (leg.getMode().equals("car")) {
                                leg.setRoute(null);
                                leg.setMode("pt");
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyEVOwnership() {
        Double evOwnershipRate = populationConfig.getEvOwnershipRate();

        if (evOwnershipRate == null) {
            logger.info("Not setting any EV ownership.");
        } else {
            logger.info(String.format("Setting EV ownership to %.2f%%", evOwnershipRate * 100));

            for (Person person : scenario.getPopulation().getPersons().values()) {
                if (!isValidPerson(person)) continue;

                if (random.nextDouble() < evOwnershipRate) {
                    person.getAttributes().putAttribute(MASModule.EV, true);
                }
            }
        }
    }

    private void applyEbikeOwnership() {
        Double ebikeOwnershipRate = populationConfig.getEbikeOwnershipRate();

        if (ebikeOwnershipRate == null) {
            logger.info("Not setting any ebike ownership.");
        } else {
            logger.info(String.format("Setting ebike ownership to %.2f%%", ebikeOwnershipRate * 100));

            for (Person person : scenario.getPopulation().getPersons().values()) {
                if (!isValidPerson(person)) continue;

                if (random.nextDouble() < ebikeOwnershipRate) {
                    person.getAttributes().putAttribute(MASModule.EBIKE, true);
                }
            }
        }
    }
}
