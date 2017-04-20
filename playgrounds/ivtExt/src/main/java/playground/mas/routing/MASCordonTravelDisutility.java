package playground.mas.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

public class MASCordonTravelDisutility {
    final private Collection<Link> cordonLinks;
    final private double cordonDisutility;

    public MASCordonTravelDisutility(Collection<Link> cordonLinks, double cordonDisutility) {
        this.cordonLinks = cordonLinks;
        this.cordonDisutility = cordonDisutility;
    }

    protected double getCordonDisutility(Link link) {
        return cordonLinks.contains(link) ? cordonDisutility : 0.0;
    }
}
