package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CountsHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Map<Id<Person>, PersonDepartureEvent> departures = new HashMap<>();
    final private Map<Id<Person>, PersonArrivalEvent> arrivals = new HashMap<>();
    final private Map<Id<Person>, PersonEntersVehicleEvent> enterVehicleEvents = new HashMap<>();

    final private Collection<Id<Link>> insideInnerCordonLinkIds;
    final private Collection<Id<Link>> insideOuterCordonLinkIds;
    final private Collection<Id<Link>> analysisLinkIds;

    final private Collection<Id<Person>> evPersonIds;

    public CountsHandler(DataFrame dataFrame, BinCalculator binCalculator, Collection<Id<Link>> insideInnerCordonLinkIds, Collection<Id<Link>> insideOuterCordonLinkIds, Collection<Id<Link>> analysisLinkIds, Collection<Id<Person>> evPersonIds) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
        this.insideInnerCordonLinkIds = insideInnerCordonLinkIds;
        this.insideOuterCordonLinkIds = insideOuterCordonLinkIds;
        this.analysisLinkIds = analysisLinkIds;
        this.evPersonIds = evPersonIds;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getPersonId().toString().startsWith("av_") || event.getPersonId().toString().startsWith("bus_") || event.getPersonId().toString().startsWith("pt_")) {
            return;
        }

        if (!departures.containsKey(event.getPersonId())) {
            departures.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        arrivals.put(event.getPersonId(), event);
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (!event.getActType().contains("interaction")) {
            PersonDepartureEvent departureEvent = departures.remove(event.getPersonId());
            PersonArrivalEvent arrivalEvent = arrivals.remove(event.getPersonId());
            PersonEntersVehicleEvent entersVehicleEvent = enterVehicleEvents.remove(event.getPersonId());

            if (departureEvent != null && arrivalEvent != null) {
                String mode = departureEvent.getLegMode();
                if (mode.contains("transit")) mode = "pt";

                if (mode.equals("av")) {
                    if (entersVehicleEvent.getVehicleId().toString().contains("solo")) {
                        mode = "av_solo";
                    } else if (entersVehicleEvent.getVehicleId().toString().contains("pool")) {
                        mode = "av_pool";
                    }
                }

                if (mode.equals("car") && evPersonIds.contains(event.getPersonId())) {
                    mode = "ev";
                }

                if (binCalculator.isCoveredValue(departureEvent.getTime())) {
                    if (insideInnerCordonLinkIds.contains(departureEvent.getLinkId())) {
                        DataFrame.increment(dataFrame.insideInnerCordonDepartures, mode, binCalculator.getIndex(departureEvent.getTime()));
                    }

                    if (insideOuterCordonLinkIds.contains(departureEvent.getLinkId())) {
                        DataFrame.increment(dataFrame.insideOuterCordonDepartures, mode, binCalculator.getIndex(departureEvent.getTime()));
                    }

                    if (analysisLinkIds.contains(departureEvent.getLinkId())) {
                        DataFrame.increment(dataFrame.departures, mode, binCalculator.getIndex(departureEvent.getTime()));
                    }
                }

                if (binCalculator.isCoveredValue(arrivalEvent.getTime())) {
                    if (insideInnerCordonLinkIds.contains(arrivalEvent.getLinkId())) {
                        DataFrame.increment(dataFrame.insideInnerCordonArrivals, mode, binCalculator.getIndex(arrivalEvent.getTime()));
                    }

                    if (insideOuterCordonLinkIds.contains(arrivalEvent.getLinkId())) {
                        DataFrame.increment(dataFrame.insideOuterCordonArrivals, mode, binCalculator.getIndex(arrivalEvent.getTime()));
                    }

                    if (analysisLinkIds.contains(arrivalEvent.getLinkId())) {
                        DataFrame.increment(dataFrame.arrivals, mode, binCalculator.getIndex(arrivalEvent.getTime()));
                    }
                }
            }
        }
    }

    @Override
    public void reset(int iteration) {}

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (departures.containsKey(event.getPersonId())) {
            enterVehicleEvents.put(event.getPersonId(), event);
        }
    }
}
