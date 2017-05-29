package playground.mas.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;
import playground.mas.cordon.ChargeType;
import playground.mas.cordon.CordonPricing;
import playground.mas.cordon.CordonState;

import java.util.Collection;
import java.util.stream.Collectors;

public class MASCordonTravelDisutility {
    final private double marginalUtilityOfMoney;
    final private CordonPricing cordonPricing;

    public MASCordonTravelDisutility(double marginalUtilityOfMoney, CordonPricing cordonPricing) {
        this.marginalUtilityOfMoney = marginalUtilityOfMoney;
        this.cordonPricing = cordonPricing;
    }

    protected double getCordonDisutility(Link link, double time, ChargeType type) {
        return marginalUtilityOfMoney * cordonPricing.getFee(link, type, time);
    }
}
