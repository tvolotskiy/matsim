package playground.mas.dispatcher;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import playground.mas.MASConfigGroup;
import playground.mas.routing.MASAVTravelDisutility;
import playground.mas.routing.MASCordonTravelDisutility;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculatorFactory;

public class MASRouterFactory implements ParallelLeastCostPathCalculatorFactory {
    final private boolean isSolo;

    final private Network network;
    final private TravelTime travelTime;
    final private MASCordonTravelDisutility cordonDisutility;

    public MASRouterFactory(Network network, TravelTime travelTime, MASCordonTravelDisutility cordonDisutility, boolean isSolo) {
        this.network = network;
        this.travelTime = travelTime;
        this.cordonDisutility = cordonDisutility;
        this.isSolo = isSolo;
    }

    @Override
    public LeastCostPathCalculator createRouter() {
        TravelDisutility travelDisutility = new MASAVTravelDisutility(isSolo, new OnlyTimeDependentTravelDisutility(travelTime), cordonDisutility);
        return new Dijkstra(network, travelDisutility, travelTime);
    }
}