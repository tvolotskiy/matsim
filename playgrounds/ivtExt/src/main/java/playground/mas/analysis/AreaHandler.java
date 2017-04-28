package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AreaHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
    final private Collection<Id<Link>> areaLinkIds;
    final private DataFrame dataFrame;

    final private Map<Id<Person>, PersonDepartureEvent> depatures = new HashMap<>();
    final private Map<Id<Person>, PersonArrivalEvent> arrivals = new HashMap<>();

    public AreaHandler(Collection<Id<Link>> areaLinkIds, DataFrame dataFrame) {
        this.dataFrame = dataFrame;
        this.areaLinkIds = areaLinkIds;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (!depatures.containsKey(event.getPersonId())) {
            depatures.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        arrivals.put(event.getPersonId(), event);
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (!event.getActType().contains("interaction")) {
            PersonDepartureEvent departureEvent = depatures.remove(event.getPersonId());
            PersonArrivalEvent arrivalEvent = arrivals.remove(event.getPersonId());
        }
    }

    @Override
    public void reset(int iteration) {}
}
