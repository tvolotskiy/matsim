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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import playground.mas.MASAttributeUtils;
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

    final private Network network;

    final private Population population;

    public CountsHandler(DataFrame dataFrame, BinCalculator binCalculator, Network network, Population population) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
        this.network = network;
        this.population = population;
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

                if (mode.equals("car") && MASAttributeUtils.isEVUser(population.getPersons().get(event.getPersonId()))) {
                    mode = "ev";
                }

                if (binCalculator.isCoveredValue(departureEvent.getTime())) {
                    Link link = network.getLinks().get(departureEvent.getLinkId());

                    if (MASAttributeUtils.isInnerCordon(link)) {
                        DataFrame.increment(dataFrame.insideInnerCordonDepartures, mode, binCalculator.getIndex(departureEvent.getTime()));
                    }

                    if (RunAnalysis.isInsideOuterCordon(link)) {
                        DataFrame.increment(dataFrame.insideOuterCordonDepartures, mode, binCalculator.getIndex(departureEvent.getTime()));
                    }

                    if (RunAnalysis.isAnalysisLink(link)) {
                        DataFrame.increment(dataFrame.departures, mode, binCalculator.getIndex(departureEvent.getTime()));
                    }
                }

                if (binCalculator.isCoveredValue(arrivalEvent.getTime())) {
                    Link link = network.getLinks().get(arrivalEvent.getLinkId());

                    if (MASAttributeUtils.isInnerCordon(link)) {
                        DataFrame.increment(dataFrame.insideInnerCordonArrivals, mode, binCalculator.getIndex(arrivalEvent.getTime()));
                    }

                    if (RunAnalysis.isInsideOuterCordon(link)) {
                        DataFrame.increment(dataFrame.insideOuterCordonArrivals, mode, binCalculator.getIndex(arrivalEvent.getTime()));
                    }

                    if (RunAnalysis.isAnalysisLink(link)) {
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
