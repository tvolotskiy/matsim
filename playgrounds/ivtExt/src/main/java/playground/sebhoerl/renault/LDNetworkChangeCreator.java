package playground.sebhoerl.renault;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.Counter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LDNetworkChangeCreator {
    public static void main(String[] args) throws IOException {
        final int endTime = 30 * 3600;
        final int binTime = 15 * 60;
        final double minSpeed = 1.38;

        String networkPath = args[0];
        String inputEventsPath = args[1];
        String filterPath = args[2];
        String outputEventsPath = args[3];

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkPath);

        Set<Id<Node>> filter = new HashSet<Id<Node>>();
        LDFilterReader filterReader = new LDFilterReader(filter);
        filterReader.read(new File(filterPath));

        EventsManager events = EventsUtils.createEventsManager();

        TravelTimeCalculator calculator = new TravelTimeCalculator(network, binTime, endTime, new TravelTimeCalculatorConfigGroup());
        events.addHandler(calculator);

        new MatsimEventsReader(events).readFile(inputEventsPath);

        List<NetworkChangeEvent> changeEvents = new LinkedList<>();
        Counter counter = new Counter("Link Change Event");

        for (Link link : network.getLinks().values()) {
            if (!link.getAllowedModes().contains("car")) continue;
            if (filter.contains(link.getFromNode().getId())) continue;
            if (filter.contains(link.getToNode().getId())) continue;

            for (int i = 0; i < calculator.getNumSlots(); i++) {
                double time = i * calculator.getTimeSlice();
                double speed = Math.max(minSpeed, link.getLength() / calculator.getLinkTravelTime(link.getId(), time));

                if (Double.isInfinite(speed) || Double.isNaN(speed)) {
                    speed = link.getFreespeed();
                }

                NetworkChangeEvent changeEvent = new NetworkChangeEvent(time);
                changeEvent.addLink(link);
                changeEvent.setFreespeedChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, speed));

                changeEvents.add(changeEvent);
                counter.incCounter();
            }
        }

        NetworkChangeEventsWriter eventsWriter = new NetworkChangeEventsWriter();
        eventsWriter.write(outputEventsPath, changeEvents);
    }
}
