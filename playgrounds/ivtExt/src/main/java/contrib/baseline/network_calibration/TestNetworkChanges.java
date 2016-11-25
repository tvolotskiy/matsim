package contrib.baseline.network_calibration;

import com.google.inject.Inject;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestNetworkChanges {
    /**
     * Shows that one can simply change the freespeeds in the network directly
     */

    final static String source = "/home/sebastian/Downloads/matsim/examples/equil/config.xml";

    static public class NetworkChangeListener implements IterationEndsListener {
        @Inject Scenario scenario;

        @Override
        public void notifyIterationEnds(IterationEndsEvent event) {
            if (event.getIteration() % 10 == 0) {
                for (Link link : scenario.getNetwork().getLinks().values()) {
                    link.setFreespeed(link.getFreespeed() * 1.5);
                }
            }
        }
    }

    static public class TimeCollector implements LinkEnterEventHandler, LinkLeaveEventHandler {
        final private Map<Id<Vehicle>, LinkEnterEvent> events = new HashMap<>();

        private double totalTime = 0.0;
        private double totalCount = 0.0;

        @Override
        public void handleEvent(LinkEnterEvent enter) {
            events.put(enter.getVehicleId(), enter);
        }

        @Override
        public void handleEvent(LinkLeaveEvent leave) {
            LinkEnterEvent enter = events.remove(leave.getVehicleId());

            if (enter != null) {
                totalTime += leave.getTime() - enter.getTime();
                totalCount += 1.0;
            }
        }

        @Override
        public void reset(int iteration) {
            if (totalCount > 0.0) {
                System.err.println("Average travel time " + iteration + ": " + String.valueOf(totalTime / totalCount));
            } else {
                System.err.println("First iteration");
            }

            events.clear();
            totalCount = 0.0;
            totalTime = 0.0;
        }
    }

    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.OFF);

        Config config = ConfigUtils.loadConfig(source);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(100);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controller = new Controler(scenario);

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addControlerListenerBinding().to(NetworkChangeListener.class);
                this.addEventHandlerBinding().to(TimeCollector.class);
            }
        });

        controller.run();
    }
}
