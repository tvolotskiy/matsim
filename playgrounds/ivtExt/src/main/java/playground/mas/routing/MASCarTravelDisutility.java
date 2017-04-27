package playground.mas.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

public class MASCarTravelDisutility implements TravelDisutility {
    final private TravelDisutility delegate;
    final private Collection<Id<Person>> evUserIds;
    final private MASCordonTravelDisutility cordonDisutility;

    public MASCarTravelDisutility(TravelDisutility carTravelDisutility, Collection<Id<Person>> evUserIds, MASCordonTravelDisutility cordonDisutility) {
        this.delegate = carTravelDisutility;
        this.evUserIds = evUserIds;
        this.cordonDisutility = cordonDisutility;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        return delegate.getLinkTravelDisutility(link, time, person, vehicle) + (evUserIds.contains(person.getId()) ? 0.0 : cordonDisutility.getCordonDisutility(link));
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0.0;
    }
}
