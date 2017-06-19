package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;
import playground.mas.MASAttributeUtils;
import playground.mas.cordon.*;
import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.avtaxi.data.AVOperator;

import javax.xml.crypto.Data;
import java.util.*;

public class CordonHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler {
    final private CordonPricing cordonPricing;
    final private ChargeTypeFinder chargeTypeFinder;

    final private Network network;

    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Set<Id<Person>> departures = new HashSet<>();
    final private Map<Id<Vehicle>, Set<Id<Person>>> passengers = new HashMap<>();

    public CordonHandler(DataFrame dataFrame, BinCalculator binCalculator, CordonPricing cordonPricing, ChargeTypeFinder chargeTypeFinder, Network network) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;

        this.cordonPricing = cordonPricing;
        this.chargeTypeFinder = chargeTypeFinder;
        this.network = network;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (chargeTypeFinder.mayAffectDeparture(event.getLegMode(), event.getPersonId())) {
            departures.add(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (departures.remove(event.getPersonId())) {
            if (!passengers.containsKey(event.getVehicleId())) {
                passengers.put(event.getVehicleId(), new HashSet<>());
            }

            passengers.get(event.getVehicleId()).add(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        Set<Id<Person>> vehiclePassengers = passengers.get(event.getVehicleId());

        if (vehiclePassengers != null) {
            vehiclePassengers.remove(event.getPersonId());

            if (vehiclePassengers.size() == 0) {
                passengers.remove(event.getVehicleId());
            }
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (cordonPricing.isAffectingLink(event.getLinkId())) {
            Set<Id<Person>> vehiclePassengers = passengers.get(event.getVehicleId());
            Link link = network.getLinks().get(event.getLinkId());

            if (MASAttributeUtils.isOuterCordon(link)) {
                if (binCalculator.isCoveredValue(event.getTime())) {
                    DataFrame.increment(dataFrame.outerCordonCrossings, binCalculator.getIndex(event.getTime()));
                }
            }

            if (MASAttributeUtils.isInnerCordon(link)) {
                if (binCalculator.isCoveredValue(event.getTime())) {
                    DataFrame.increment(dataFrame.innerCordonDistance, binCalculator.getIndex(event.getTime()), link.getLength());
                }
            }

            if (vehiclePassengers != null) {
                for (Id<Person> passengerId : vehiclePassengers) {
                    ChargeType chargeType = chargeTypeFinder.getChargeType(passengerId, event.getVehicleId());

                    if (cordonPricing.getFee(event.getLinkId(), chargeType, event.getTime()) > 0.0) {
                        if (MASAttributeUtils.isOuterCordon(link)) {
                            DataFrame.increment(dataFrame.chargeableOuterCordonCrossings, binCalculator.getIndex(event.getTime()));
                            DataFrame.increment(dataFrame.chargeableOuterCordonCrossingsByMode, chargeType, binCalculator.getIndex(event.getTime()));
                        }

                        if (MASAttributeUtils.isInnerCordon(link)) {
                            DataFrame.increment(dataFrame.chargeableInnerCordonDistance, binCalculator.getIndex(event.getTime()), link.getLength());
                            DataFrame.increment(dataFrame.chargeableInnerCordonDistanceByMode, chargeType, binCalculator.getIndex(event.getTime()), link.getLength());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void reset(int iteration) {}

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        departures.remove(event.getPersonId());
    }
}
