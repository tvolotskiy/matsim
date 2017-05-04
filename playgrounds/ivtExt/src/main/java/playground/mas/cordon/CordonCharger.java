package playground.mas.cordon;

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
import org.matsim.vehicles.Vehicle;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.*;

public class CordonCharger implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler {
    final private Map<Id<Person>, Double> charges = new HashMap<>();

    final private Set<Id<Person>> departures = new HashSet<>();
    final private Map<Id<Vehicle>, Id<Person>> passengers = new HashMap<>();

    final private Collection<Id<Person>> evUserIds;
    final private Collection<Id<Link>> cordonLinkIds;
    final private Collection<Id<AVOperator>> chargedOperators;

    final private double cordonPrice;
    final private CordonState cordonState;

    public CordonCharger(CordonState cordonState, Collection<Id<Link>> cordonLinkIds, double cordonPrice, Collection<Id<AVOperator>> chargedOperators, Collection<Id<Person>> evUserIds) {
        this.cordonLinkIds = cordonLinkIds;
        this.cordonPrice = cordonPrice;
        this.chargedOperators = chargedOperators;
        this.evUserIds = evUserIds;
        this.cordonState = cordonState;
    }

    @Override
    public void reset(int iteration) {
        passengers.clear();
        charges.clear();
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (MASCordonUtils.isChargeableDeparture(event.getPersonId(), event.getLegMode(), evUserIds)) {
            departures.add(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (departures.remove(event.getPersonId())) {
            if (MASCordonUtils.isPrivateVehicle(event.getVehicleId()) || MASCordonUtils.isChargeableOperator(event.getVehicleId(), chargedOperators)) {
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

            if (passengerId != null && cordonState.isCordonActive(event.getTime())) {
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
