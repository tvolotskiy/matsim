package playground.mas.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class MASAVTravelDisutility implements TravelDisutility {
    final private TravelDisutility delegate;
    final private MASCordonTravelDisutility cordonDisutility;

    public MASAVTravelDisutility(TravelDisutility avTravelDisutility, MASCordonTravelDisutility cordonDisutility) {
        this.delegate = avTravelDisutility;
        this.cordonDisutility = cordonDisutility;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return delegate.getLinkTravelDisutility(link, time, person, vehicle) + cordonDisutility.getCordonDisutility(link);
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return delegate.getLinkMinimumTravelDisutility(link);
    }
}
