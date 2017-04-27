package playground.mas.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;
import java.util.stream.Collectors;

public class MASCordonTravelDisutility {
    final private Collection<Id<Link>> cordonLinkIds;
    final private double cordonDisutility;

    public MASCordonTravelDisutility(Collection<Id<Link>> cordonLinkIds, double cordonDisutility) {
        this.cordonDisutility = cordonDisutility;
        this.cordonLinkIds = cordonLinkIds;
    }

    protected double getCordonDisutility(Link link) {
        return cordonLinkIds.contains(link.getId()) ? cordonDisutility : 0.0;
    }
}
