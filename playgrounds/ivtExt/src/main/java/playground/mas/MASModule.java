package playground.mas;

import com.google.inject.Key;
import com.google.inject.Provider;
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
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import playground.mas.dispatcher.MASPoolDispatcherFactory;
import playground.mas.dispatcher.MASSoloDispatcherFactory;
import playground.mas.routing.MASCarTravelDisutility;
import playground.mas.routing.MASCarTravelDisutilityFactory;
import playground.mas.routing.MASCordonTravelDisutility;
import playground.mas.scoring.MASScoringFunctionFactory;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.zurich_av.RunZurichWithAV;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MASModule extends AbstractModule {
    final public static String CORDON_LINKS = "cordon_links";
    final private Logger log = Logger.getLogger(MASModule.class);

    @Override
    public void install() {
        addEventHandlerBinding().to(CordonCharger.class);

        bind(ScoringFunctionFactory.class).to(MASScoringFunctionFactory.class);

        // Override travel disutility factories for proper routing with cordon
        addTravelDisutilityFactoryBinding(TransportMode.car).to(MASCarTravelDisutilityFactory.class);

        AVUtils.bindDispatcherFactory(binder(), "MAS_Solo").to(MASSoloDispatcherFactory.class);
        AVUtils.bindDispatcherFactory(binder(), "MAS_Pool").to(MASPoolDispatcherFactory.class);
    }

    @Provides @Singleton
    private MASCarTravelDisutilityFactory provideMASCarTravelDisutilityFactory(@Named("ev_user_ids") Collection<Id<Person>> evUserIds, MASCordonTravelDisutility cordonDisutility, PlanCalcScoreConfigGroup scoreConfig) {
        TravelDisutilityFactory randomizingTravelDisutiltiyFactory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, scoreConfig);
        return new MASCarTravelDisutilityFactory(randomizingTravelDisutiltiyFactory, evUserIds, cordonDisutility);
    }

    @Provides @Singleton
    private MASCordonTravelDisutility provideMASCordonTravelDisutility(@Named(CORDON_LINKS) Collection<Id<Link>> cordonLinkIds, PlanCalcScoreConfigGroup scoreConfig, MASConfigGroup masConfig) {
        double cordonDisutility = scoreConfig.getMarginalUtilityOfMoney() * masConfig.getCordonFee();
        return new MASCordonTravelDisutility(cordonLinkIds, cordonDisutility);
    }

    @Provides @Singleton
    private CordonCharger provideCordonCharger(@Named(CORDON_LINKS) Collection<Id<Link>> cordonLinkIds, MASConfigGroup config, @Named("ev_user_ids") Collection<Id<Person>> evUserIds) {
        return new CordonCharger(cordonLinkIds, config.getCordonFee(), config.getChargedOperatorIds(), evUserIds);
    }

    @Provides @Singleton @Named(CORDON_LINKS)
    public Collection<Id<Link>> provideCordonLinkIds(Config config, MASConfigGroup masConfig, Network network) {
        return MASCordonUtils.findChargeableCordonLinks(masConfig.getCordonCenterNodeId(), masConfig.getCordonRadius(), network)
                .stream().map(l -> l.getId()).collect(Collectors.toList());
    }

    @Provides @Singleton
    public MASScoringFunctionFactory provideScoringFunctionFactory(AVScoringFunctionFactory delegate, Scenario scenario, AVConfig config, CordonCharger charger) {
        return new MASScoringFunctionFactory(delegate, scenario, charger);
    }

    @Provides @Singleton @Named("ev_user_ids")
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
