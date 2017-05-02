package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AVHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, ActivityStartEventHandler {
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Map<Id<Person>, PersonDepartureEvent> departures = new HashMap<>();
    final private Map<Id<Person>, Double> enteredActiveTime = new HashMap<>();

    public AVHandler(DataFrame dataFrame, BinCalculator binCalculator) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals("av")) {
            departures.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        departures.remove(event.getPersonId());
    }

    private String getAVMode(String stringId) {
        if (stringId.contains("solo")) {
            return "av_solo";
        } else if (stringId.contains("pool")) {
            return "av_pool";
        }

        throw new RuntimeException("Cannot infer AV mode from " + stringId);
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        PersonDepartureEvent departureEvent = departures.remove(event.getPersonId());

        if (departureEvent != null) {
            String stringId = event.getVehicleId().toString();

            if (binCalculator.isCoveredValue(departureEvent.getTime())) {
                dataFrame.waitingTimes
                        .get(getAVMode(event.getVehicleId().toString()))
                        .get(binCalculator.getIndex(departureEvent.getTime()))
                        .add(event.getTime() - departureEvent.getTime());
            }
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getPersonId().toString().startsWith("av_")) {
            if (event.getActType().equals("AVStay")) {
                Double started = enteredActiveTime.remove(event.getPersonId());

                if (started != null) {
                    for (BinCalculator.BinEntry entry : binCalculator.getBinEntriesNormalized(started, event.getTime())) {
                        DataFrame.increment(dataFrame.activeAVs, getAVMode(event.getPersonId().toString()), entry.getIndex(), entry.getWeight());
                    }
                }
            } else {
                enteredActiveTime.put(event.getPersonId(), event.getTime());
            }
        }
    }

    @Override
    public void reset(int iteration) {}
}
