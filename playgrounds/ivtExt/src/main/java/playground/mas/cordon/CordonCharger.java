package playground.mas.cordon;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.*;

public class CordonCharger implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler {
    final private Map<Id<Person>, Double> charges = new HashMap<>();

    final private Set<Id<Person>> departures = new HashSet<>();
    final private Map<Id<Vehicle>, Set<Id<Person>>> passengers = new HashMap<>();

    final private CordonPricing cordonPricing;
    final private ChargeTypeFinder chargeTypeFinder;

    public CordonCharger(CordonPricing cordonPricing, ChargeTypeFinder chargeTypeFinder) {
        this.chargeTypeFinder = chargeTypeFinder;
        this.cordonPricing = cordonPricing;
    }

    @Override
    public void reset(int iteration) {
        departures.clear();
        passengers.clear();
        charges.clear();
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

            if (vehiclePassengers != null) {
                for (Id<Person> passengerId : vehiclePassengers) {
                    ChargeType chargeType = chargeTypeFinder.getChargeType(passengerId, event.getVehicleId());
                    addCharge(passengerId, cordonPricing.getFee(event.getLinkId(), chargeType, event.getTime()));
                }
            }
        }
    }

    private void addCharge(Id<Person> passengerId, double charge) {
        Double previous = charges.get(passengerId);
        charges.put(passengerId, charge + (previous != null ? previous : 0.0));
    }

    public double getCharge(Id<Person> personId) {
        Double charge = charges.get(personId);
        return charge == null ? 0.0 : charge;
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        departures.remove(event.getPersonId());
    }
}
