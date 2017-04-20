package playground.sebhoerl.avtaxi.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculatorFactory;

@Singleton
public class AVParallelRouterFactory implements ParallelLeastCostPathCalculatorFactory {
    @Inject @Named(AVModule.AV_MODE) TravelTime travelTime;
    @Inject @Named(AVModule.AV_MODE) TravelDisutilityFactory travelDisutilityFactory;
    @Inject Network network;

    @Override
    public LeastCostPathCalculator createRouter() {
        return new Dijkstra(network, travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);
    }
}
