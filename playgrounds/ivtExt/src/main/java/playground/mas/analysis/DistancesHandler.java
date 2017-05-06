package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.avtaxi.data.AVOperator;

import javax.xml.crypto.Data;
import java.util.*;

public class DistancesHandler implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Network network;

    final private Map<Id<Vehicle>, Set<Id<Person>>> passengers = new HashMap<>();
    final private Map<Id<Vehicle>, LinkEnterEvent> enterEvents = new HashMap<>();

    final private Collection<Id<Person>> evPersonIds;
    final private Set<Id<Vehicle>> transitVehicleIds = new HashSet<>();

    DistancesHandler(DataFrame dataFrame, BinCalculator binCalculator, Network network, Collection<Id<Person>> evPersonIds) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
        this.network = network;
        this.evPersonIds = evPersonIds;
    }

    private String findModeForVehicle(Id<Vehicle> vehicleId) {
        String id = vehicleId.toString();

        if (id.startsWith("av_")) {
            if (id.contains("solo")) {
                return "av_solo";
            } else if (id.contains("pool")) {
                return "av_pool";
            } else {
                return "av";
            }
        } else if (id.startsWith("bus_") || id.startsWith("pt_") || transitVehicleIds.contains(vehicleId)) {
            return "pt";
        } else {
            return "car";
        }
    }

    private boolean isValidAgent(Id<Person> personId) {
        String stringId = personId.toString();

        if (stringId.startsWith("av_") || stringId.startsWith("bus_") || stringId.startsWith("pt_")) {
            return false;
        }

        return true;
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (!passengers.containsKey(event.getVehicleId())) {
            passengers.put(event.getVehicleId(), new HashSet<>());
        }

        if (isValidAgent(event.getPersonId())) {
            passengers.get(event.getVehicleId()).add(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        passengers.get(event.getVehicleId()).remove(event.getPersonId());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        enterEvents.put(event.getVehicleId(), event);
    }

    private double getLinkLength(Id<Link> linkId) {
        return network.getLinks().get(linkId).getLength();
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        LinkEnterEvent enterEvent = enterEvents.remove(event.getVehicleId());

        if (enterEvent != null) {
            String mode = findModeForVehicle(event.getVehicleId());

            double length = getLinkLength(event.getLinkId());
            long occupancy = 0;

            if (passengers.containsKey(event.getVehicleId())) {
                occupancy = passengers.get(event.getVehicleId()).size();

                if (mode.equals("car") && occupancy == 1) {
                    Id<Person> driverId = passengers.get(event.getVehicleId()).stream().findAny().get();

                    if (evPersonIds.contains(driverId)) {
                        mode = "ev";
                    }
                }
            }

            List<Double> occupancySlot = mode.equals("av_pool") ? dataFrame.distanceByPoolOccupancy.get(occupancy) : null;

            if (binCalculator.isCoveredValue(enterEvent.getTime())) {
                DataFrame.increment(dataFrame.vehicleDistances, mode, binCalculator.getIndex(enterEvent.getTime()), length);
                DataFrame.increment(dataFrame.passengerDistances, mode, binCalculator.getIndex(enterEvent.getTime()), length * occupancy);
                if (occupancySlot != null) DataFrame.increment(occupancySlot, binCalculator.getIndex(enterEvent.getTime()), length);
            }
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        enterEvents.remove(event.getVehicleId());
    }

    @Override
    public void reset(int iteration) {}

    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        transitVehicleIds.add(event.getVehicleId());
    }
}
