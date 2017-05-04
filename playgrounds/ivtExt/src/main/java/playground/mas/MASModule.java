package playground.mas;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import playground.mas.cordon.CordonCharger;
import playground.mas.cordon.CordonState;
import playground.mas.cordon.IntervalCordonState;
import playground.mas.cordon.MASCordonUtils;
import playground.mas.dispatcher.MASPoolDispatcherFactory;
import playground.mas.dispatcher.MASSoloDispatcherFactory;
import playground.mas.routing.MASCarTravelDisutilityFactory;
import playground.mas.routing.MASCordonTravelDisutility;
import playground.mas.scoring.MASScoringFunctionFactory;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MASModule extends AbstractModule {
    final public static String CORDON_LINKS = "cordon_links";
    final public static String EV_USER_IDS = "ev_user_ids";

    final private Logger log = Logger.getLogger(MASModule.class);

    @Override
    public void install() {
        addEventHandlerBinding().to(CordonCharger.class);

        bind(ScoringFunctionFactory.class).to(MASScoringFunctionFactory.class);

        // Override travel disutility factories for proper routing with cordon
        addTravelDisutilityFactoryBinding(TransportMode.car).to(MASCarTravelDisutilityFactory.class);

        AVUtils.bindDispatcherFactory(binder(), "MAS_Solo").to(MASSoloDispatcherFactory.class);
        AVUtils.bindDispatcherFactory(binder(), "MAS_Pool").to(MASPoolDispatcherFactory.class);

        bind(CordonState.class).to(IntervalCordonState.class);
    }

    @Provides @Singleton
    private MASCarTravelDisutilityFactory provideMASCarTravelDisutilityFactory(@Named("ev_user_ids") Collection<Id<Person>> evUserIds, MASCordonTravelDisutility cordonDisutility, PlanCalcScoreConfigGroup scoreConfig) {
        TravelDisutilityFactory randomizingTravelDisutiltiyFactory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, scoreConfig);
        return new MASCarTravelDisutilityFactory(randomizingTravelDisutiltiyFactory, evUserIds, cordonDisutility);
    }

    @Provides @Singleton
    private IntervalCordonState provideIntervalCordonState(MASConfigGroup masConfigGroup) {
        IntervalCordonState intervalCordonState = new IntervalCordonState();

        IntervalCordonState.Reader reader = new IntervalCordonState.Reader(intervalCordonState);
        reader.read(masConfigGroup.getCordonIntervals());

        for (Tuple<Double, Double> interval : intervalCordonState.getIntervals()) {
            log.info("Adding cordon charge interval from " + Time.writeTime(interval.getFirst()) + " to " + Time.writeTime(interval.getSecond()));
        }

        return intervalCordonState;
    }

    @Provides @Singleton
    private MASCordonTravelDisutility provideMASCordonTravelDisutility(CordonState cordonState, @Named(CORDON_LINKS) Collection<Id<Link>> cordonLinkIds, PlanCalcScoreConfigGroup scoreConfig, MASConfigGroup masConfig) {
        double cordonDisutility = scoreConfig.getMarginalUtilityOfMoney() * masConfig.getCordonFee();
        return new MASCordonTravelDisutility(cordonState, cordonLinkIds, cordonDisutility);
    }

    @Provides @Singleton
    private CordonCharger provideCordonCharger(CordonState cordonState, @Named(CORDON_LINKS) Collection<Id<Link>> cordonLinkIds, MASConfigGroup config, @Named("ev_user_ids") Collection<Id<Person>> evUserIds) {
        return new CordonCharger(cordonState, cordonLinkIds, config.getCordonFee(), config.getChargedOperatorIds(), evUserIds);
    }

    @Provides @Singleton @Named(CORDON_LINKS)
    public Collection<Id<Link>> provideCordonLinkIds(MASConfigGroup masConfig, Network network) {
        return MASCordonUtils.findChargeableCordonLinks(masConfig.getCordonCenterNodeId(), masConfig.getCordonRadius(), network)
                .stream().map(l -> l.getId()).collect(Collectors.toList());
    }

    @Provides @Singleton
    public MASScoringFunctionFactory provideScoringFunctionFactory(AVScoringFunctionFactory delegate, Scenario scenario, MASConfigGroup masConfig, CordonCharger charger, @Named(EV_USER_IDS) Collection<Id<Person>> evUserIds) {
        return new MASScoringFunctionFactory(delegate, scenario, charger, masConfig.getAdditionalEVCostsPerKm(), evUserIds);
    }

    @Provides @Singleton @Named(EV_USER_IDS)
    public Collection<Id<Person>> provideEVUserIds(Population population) {
        return population.getPersons().keySet().stream().filter(new Predicate<Id<Person>>() {
            @Override
            public boolean test(Id<Person> personId) {
                Boolean flag = (Boolean) population.getPersonAttributes().getAttribute(personId.toString(), "ev");
                return flag != null && flag;
            }
        }).collect(Collectors.toSet());
    }
}
