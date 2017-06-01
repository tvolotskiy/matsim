package playground.mas.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;
import playground.mas.cordon.ChargeType;
import playground.mas.MASAttributeUtils;

public class MASCarTravelDisutility implements TravelDisutility {
    final private TravelDisutility delegate;
    final private MASCordonTravelDisutility cordonDisutility;

    public MASCarTravelDisutility(TravelDisutility carTravelDisutility, MASCordonTravelDisutility cordonDisutility) {
        this.delegate = carTravelDisutility;
        this.cordonDisutility = cordonDisutility;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return delegate.getLinkTravelDisutility(link, time, person, vehicle) + (MASAttributeUtils.isEVUser(person) ?
                cordonDisutility.getCordonDisutility(link, time, ChargeType.EV)
                : cordonDisutility.getCordonDisutility(link, time, ChargeType.CAR));
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0.0;
    }
}
