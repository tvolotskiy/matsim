package playground.mas;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import playground.mas.cordon.*;
import playground.mas.dispatcher.MASPoolDispatcherFactory;
import playground.mas.dispatcher.MASRouterFactory;
import playground.mas.dispatcher.MASSoloDispatcherFactory;
import playground.mas.replanning.MASPermissibleModesCalculator;
import playground.mas.routing.MASCarTravelDisutilityFactory;
import playground.mas.routing.MASCordonTravelDisutility;
import playground.mas.scoring.MASScoringFunctionFactory;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MASModule extends AbstractModule {
    final public static String EV_USER_IDS = "ev_user_ids";
    final public static String OUTER_CORDON = "outer_cordon";
    final public static String INNER_CORDON = "inner_cordon";

    final public static String AV_POOL_OPERATOR = "pool";
    final public static String AV_SOLO_OPERATOR = "solo";

    final public static String EBIKE = "ebike";
    final public static String EV = "ev";

    final static private Logger log = Logger.getLogger(MASModule.class);

    static public void applyEbikes(Config config) {
        PlansCalcRouteConfigGroup plansCalcRouteConfigGroup = (PlansCalcRouteConfigGroup) config.getModules().get(PlansCalcRouteConfigGroup.GROUP_NAME);
        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = (PlanCalcScoreConfigGroup) config.getModules().get(PlanCalcScoreConfigGroup.GROUP_NAME);

        MASConfigGroup masConfigGroup = (MASConfigGroup) config.getModules().get(MASConfigGroup.MAS);

        {
            // Be careful! Somehow this stuff does not work with the old config format (v1)!

            PlansCalcRouteConfigGroup.ModeRoutingParams bikeParams = plansCalcRouteConfigGroup.getModeRoutingParams().get(TransportMode.bike);
            PlansCalcRouteConfigGroup.ModeRoutingParams ebikeParams = new PlansCalcRouteConfigGroup.ModeRoutingParams(MASModule.EBIKE);

            ebikeParams.setTeleportedModeSpeed(bikeParams.getTeleportedModeSpeed() * masConfigGroup.getEbikeSpeedupFactor());
            ebikeParams.setBeelineDistanceFactor(bikeParams.getBeelineDistanceFactor());

            plansCalcRouteConfigGroup.addModeRoutingParams(ebikeParams);

            log.info("Created ebike mode with speed " + ebikeParams.getTeleportedModeSpeed() + " (original bike at " + bikeParams.getTeleportedModeSpeed() + ")");
        }

        {
            PlanCalcScoreConfigGroup.ModeParams bikeParams = planCalcScoreConfigGroup.getModes().get(TransportMode.bike);
            PlanCalcScoreConfigGroup.ModeParams ebikeParams = new PlanCalcScoreConfigGroup.ModeParams(MASModule.EBIKE);

            ebikeParams.setMarginalUtilityOfTraveling(bikeParams.getMarginalUtilityOfTraveling());

            planCalcScoreConfigGroup.addModeParams(ebikeParams);
        }
    }

    @Override
    public void install() {
        addEventHandlerBinding().to(CordonCharger.class);

        bind(ScoringFunctionFactory.class).to(MASScoringFunctionFactory.class);

        // Override travel disutility factories for proper routing with cordon
        addTravelDisutilityFactoryBinding(TransportMode.car).to(MASCarTravelDisutilityFactory.class);

        AVUtils.bindDispatcherFactory(binder(), "MAS_Solo").to(MASSoloDispatcherFactory.class);
        AVUtils.bindDispatcherFactory(binder(), "MAS_Pool").to(MASPoolDispatcherFactory.class);

        addControlerListenerBinding().to(MASConsistencyListener.class);

        addControlerListenerBinding().to(Key.get(ParallelLeastCostPathCalculator.class, Names.named("av_solo")));
        addControlerListenerBinding().to(Key.get(ParallelLeastCostPathCalculator.class, Names.named("av_pool")));
    }

    @Provides @Singleton
    private MASCarTravelDisutilityFactory provideMASCarTravelDisutilityFactory(MASCordonTravelDisutility cordonDisutility, PlanCalcScoreConfigGroup scoreConfig) {
        TravelDisutilityFactory randomizingTravelDisutiltiyFactory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, scoreConfig);
        return new MASCarTravelDisutilityFactory(randomizingTravelDisutiltiyFactory, cordonDisutility);
    }

    @Provides @Singleton @Named(OUTER_CORDON)
    public CordonState provideOuterCordonState(MASConfigGroup masConfigGroup) {
        IntervalCordonState intervalCordonState = new IntervalCordonState();

        IntervalCordonState.Reader reader = new IntervalCordonState.Reader(intervalCordonState);
        reader.read(masConfigGroup.getOuterCordonIntervals());

        for (Tuple<Double, Double> interval : intervalCordonState.getIntervals()) {
            log.info("Adding cordon charge interval from " + Time.writeTime(interval.getFirst()) + " to " + Time.writeTime(interval.getSecond()));
        }

        return intervalCordonState;
    }

    @Provides @Singleton @Named(INNER_CORDON)
    public CordonState provideInnerCordonState(MASConfigGroup masConfigGroup) {
        IntervalCordonState intervalCordonState = new IntervalCordonState();

        IntervalCordonState.Reader reader = new IntervalCordonState.Reader(intervalCordonState);
        reader.read(masConfigGroup.getInnerCordonIntervals());

        for (Tuple<Double, Double> interval : intervalCordonState.getIntervals()) {
            log.info("Adding cordon charge interval from " + Time.writeTime(interval.getFirst()) + " to " + Time.writeTime(interval.getSecond()));
        }

        return intervalCordonState;
    }

    @Provides @Singleton
    private MASCordonTravelDisutility provideMASCordonTravelDisutility(PlanCalcScoreConfigGroup scoreConfig, CordonPricing cordonPricing) {
        return new MASCordonTravelDisutility(scoreConfig.getMarginalUtilityOfMoney(), cordonPricing);
    }

    @Provides @Singleton
    private CordonCharger provideCordonCharger(CordonPricing cordonPricing, ChargeTypeFinder chargeTypeFinder) {
        return new CordonCharger(cordonPricing, chargeTypeFinder);
    }

    @Provides @Singleton
    private CordonPricing provideCordonPricing(MASConfigGroup masConfigGroup, Network network, @Named(INNER_CORDON) CordonState innerCordonState, @Named(OUTER_CORDON) CordonState outerCordonState) {
        return new CordonPricing(masConfigGroup, network, innerCordonState, outerCordonState);
    }

    @Provides @Singleton
    private ChargeTypeFinder provideChargeTypeFinder(Population population) {
        return new ChargeTypeFinder(population);
    }

    @Provides @Singleton
    public MASScoringFunctionFactory provideScoringFunctionFactory(AVScoringFunctionFactory delegate, Scenario scenario, MASConfigGroup masConfig, CordonCharger charger) {
        return new MASScoringFunctionFactory(delegate, scenario, charger, masConfig.getAdditionalEVCostsPerKm());
    }

    @Provides @Singleton
    public PermissibleModesCalculator providePermissibleModesCalculator(SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup) {
        return new MASPermissibleModesCalculator(new PermissibleModesCalculatorImpl(subtourModeChoiceConfigGroup));
    }

    @Provides @Singleton @Named("av_solo")
    ParallelLeastCostPathCalculator provideParallelLeastCostPathCalculatorForSolo(AVConfigGroup avConfig, Network network, @Named(AVModule.AV_MODE) TravelTime travelTime, MASCordonTravelDisutility cordonDisutility) {
        return new ParallelLeastCostPathCalculator(
                (int) avConfig.getParallelRouters(),
                new MASRouterFactory(network, travelTime, cordonDisutility, true)
        );
    }

    @Provides @Singleton @Named("av_pool")
    ParallelLeastCostPathCalculator provideParallelLeastCostPathCalculatorForPool(AVConfigGroup avConfig, Network network, @Named(AVModule.AV_MODE) TravelTime travelTime, MASCordonTravelDisutility cordonDisutility) {
        return new ParallelLeastCostPathCalculator(
                (int) avConfig.getParallelRouters(),
                new MASRouterFactory(network, travelTime, cordonDisutility, false)
        );
    }
}
