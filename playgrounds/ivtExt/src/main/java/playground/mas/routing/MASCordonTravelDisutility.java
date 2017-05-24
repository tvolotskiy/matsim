package playground.mas.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;
import playground.mas.cordon.CordonState;

import java.util.Collection;
import java.util.stream.Collectors;

public class MASCordonTravelDisutility {
    final private Collection<Id<Link>> cordonLinkIds;
    final private double marginalUtilityOfMoney;
    final private double carCordonFee;
    final private double avCordonFee;
    final private double evCordonFee;
    final private CordonState cordonState;

    public enum Type {
        AV, EV, CAR
    }

    public MASCordonTravelDisutility(CordonState cordonState, Collection<Id<Link>> cordonLinkIds, double marginalUtilityOfMoney, double carCordonFee, double evCordonFee, double avCordonFee) {
        this.marginalUtilityOfMoney = marginalUtilityOfMoney;
        this.cordonLinkIds = cordonLinkIds;
        this.cordonState = cordonState;
        this.carCordonFee = carCordonFee;
        this.avCordonFee = avCordonFee;
        this.evCordonFee = evCordonFee;
    }

    protected double getCordonDisutility(Link link, double time, Type type) {
        double fee = 0.0;

        switch (type) {
            case AV: fee = avCordonFee; break;
            case EV: fee = evCordonFee; break;
            case CAR: fee = carCordonFee; break;
        }

        return (cordonLinkIds.contains(link.getId()) && cordonState.isCordonActive(time)) ? marginalUtilityOfMoney * fee : 0.0;
    }
}
