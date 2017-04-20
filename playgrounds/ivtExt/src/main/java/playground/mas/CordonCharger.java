package playground.mas;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.vehicles.Vehicle;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.*;
import java.util.stream.Collectors;

public class CordonCharger implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler {
    final private Map<Id<Person>, Double> charges = new HashMap<>();

    final private Set<Id<Person>> departures = new HashSet<>();
    final private Map<Id<Vehicle>, Id<Person>> passengers = new HashMap<>();

    final private Collection<Id<Link>> cordonLinkIds;
    final private Collection<String> chargedOperators;

    final private double cordonPrice;

    public CordonCharger(Collection<Link> cordonLinks, double cordonPrice, Collection<Id<AVOperator>> chargedOperators) {
        this.cordonLinkIds = cordonLinks.stream().map(l -> l.getId()).collect(Collectors.toSet());
        this.cordonPrice = cordonPrice;
        this.chargedOperators = chargedOperators.stream().map(i -> i.toString()).collect(Collectors.toSet());
    }

    @Override
    public void reset(int iteration) {
        passengers.clear();
        charges.clear();
    }

    private boolean isChargeableDeparture(Id<Person> personId, String mode) {
        // TODO add electric person attribute here
        return mode.equals(AVModule.AV_MODE) || mode.equals(TransportMode.car);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (isChargeableDeparture(event.getPersonId(), event.getLegMode())) {
            departures.add(event.getPersonId());
        }
    }

    private boolean isChargeableOperatorVehicle(Id<Vehicle> vehicleId) {
        String stringId = vehicleId.toString();

        if (stringId.startsWith("av_")) {
            for (String operatorId : chargedOperators) {
                if (stringId.startsWith("av_" + operatorId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isPrivateVehicle(Id<Vehicle> vehicleId) {
        return !vehicleId.toString().startsWith("av_");
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (departures.remove(event.getPersonId())) {
            if (isPrivateVehicle(event.getVehicleId()) || isChargeableOperatorVehicle(event.getVehicleId())) {
                passengers.put(event.getVehicleId(), event.getPersonId());
            }
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        passengers.remove(event.getVehicleId());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (cordonLinkIds.contains(event.getLinkId())) {
            Id<Person> passengerId = passengers.get(event.getVehicleId());

            if (passengerId != null) {
                Double charge = charges.get(passengerId);
                charge = (charge == null) ? cordonPrice : charge + cordonPrice;
                charges.put(passengerId, charge);
            }
        }
    }

    public double getCharge(Id<Person> personId) {
        Double charge = charges.get(personId);
        return charge == null ? 0.0 : charge;
    }
}
