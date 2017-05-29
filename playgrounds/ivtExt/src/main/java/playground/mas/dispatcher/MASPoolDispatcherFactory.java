package playground.mas.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import playground.mas.MASConfigGroup;
import playground.mas.routing.MASCordonTravelDisutility;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic.ParallelAggregateRideAppender;
import playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic.MultiODHeuristic;
import playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic.FactorTravelTimeEstimator;
import playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic.SerialAggregateRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;

public class MASPoolDispatcherFactory implements AVDispatcher.AVDispatcherFactory {
    @Inject private Network network;
    @Inject private EventsManager eventsManager;

    @Inject @Named(AVModule.AV_MODE)
    private TravelTime travelTime;

    @Inject private MASConfigGroup masConfig;
    @Inject private MASCordonTravelDisutility cordonDisutility;

    @Override
    public AVDispatcher createDispatcher(AVDispatcherConfig config) {
        MASRouterFactory factory = new MASRouterFactory(network, travelTime, cordonDisutility, false);
        LeastCostPathCalculator router = factory.createRouter();

        double threshold = Double.parseDouble(config.getParams().getOrDefault("aggregationThreshold", "600.0"));
        FactorTravelTimeEstimator estimator = new FactorTravelTimeEstimator(threshold);

        return new MultiODHeuristic(
                config.getParent().getId(),
                eventsManager,
                network,
                new SerialAggregateRideAppender(config, router, travelTime, estimator),
                estimator
        );
    }
}
