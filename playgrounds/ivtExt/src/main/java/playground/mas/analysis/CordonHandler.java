package playground.mas.analysis;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.events.handler.EventHandler;

public class CordonHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler {
    final private Set<Id<Person>>

    @Override
    public void handleEvent(LinkEnterEvent event) {

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {

    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {

    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {

    }

    @Override
    public void reset(int iteration) {

    }
}
