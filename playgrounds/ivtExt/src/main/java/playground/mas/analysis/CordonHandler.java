package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;
import playground.mas.cordon.CordonState;
import playground.mas.cordon.MASCordonUtils;
import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.avtaxi.data.AVOperator;

import java.util.*;

public class CordonHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler {
    final private Collection<Id<Person>> evPersonIds;
    final private Collection<Id<AVOperator>> chargedOperatorIds;
    final private Collection<Id<Link>> cordonLinkIds;

    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Map<Id<Person>, PersonDepartureEvent> departures = new HashMap<>();
    final private Set<Id<Vehicle>> vehicles = new HashSet<>();

    final private CordonState cordonState;

    public CordonHandler(DataFrame dataFrame, BinCalculator binCalculator, Collection<Id<Person>> evPersonIds, Collection<Id<AVOperator>> chargedOperatorIds, Collection<Id<Link>> cordonLinkIds, CordonState cordonState) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
        this.evPersonIds = evPersonIds;
        this.chargedOperatorIds = chargedOperatorIds;
        this.cordonLinkIds = cordonLinkIds;
        this.cordonState = cordonState;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (MASCordonUtils.isChargeableDeparture(event.getPersonId(), event.getLegMode(), evPersonIds)) {
            departures.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        PersonDepartureEvent departureEvent = departures.remove(event.getVehicleId());

        if (departureEvent != null) {
            if (MASCordonUtils.isPrivateVehicle(event.getVehicleId()) || MASCordonUtils.isChargeableOperator(event.getVehicleId(), chargedOperatorIds)) {
                vehicles.add(event.getVehicleId());
            }
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        vehicles.remove(event.getVehicleId());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (cordonLinkIds.contains(event.getLinkId())) {
            if (vehicles.contains(event.getVehicleId()) && cordonState.isCordonActive(event.getTime())) {
                if (binCalculator.isCoveredValue(event.getTime())) {
                    DataFrame.increment(dataFrame.chargeableCordonCrossings, binCalculator.getIndex(event.getTime()));
                }
            } else {
                if (binCalculator.isCoveredValue(event.getTime())) {
                    DataFrame.increment(dataFrame.cordonCrossings, binCalculator.getIndex(event.getTime()));
                }
            }
        }
    }

    @Override
    public void reset(int iteration) {}
}
