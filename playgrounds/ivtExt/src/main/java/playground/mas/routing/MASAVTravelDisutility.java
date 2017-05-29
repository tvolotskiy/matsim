package playground.mas.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;
import playground.mas.cordon.ChargeType;

public class MASAVTravelDisutility implements TravelDisutility {
    final private TravelDisutility delegate;
    final private MASCordonTravelDisutility cordonDisutility;
    final private boolean isSolo;

    public MASAVTravelDisutility(boolean isSolo, TravelDisutility avTravelDisutility, MASCordonTravelDisutility cordonDisutility) {
        this.delegate = avTravelDisutility;
        this.cordonDisutility = cordonDisutility;
        this.isSolo = isSolo;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return delegate.getLinkTravelDisutility(link, time, person, vehicle) + cordonDisutility.getCordonDisutility(link, time, isSolo ? ChargeType.AV_SOLO : ChargeType.AV_POOL);
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return delegate.getLinkMinimumTravelDisutility(link);
    }
}
