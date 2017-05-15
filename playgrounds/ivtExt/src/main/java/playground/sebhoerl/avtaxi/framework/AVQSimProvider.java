package playground.sebhoerl.avtaxi.framework;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Injector;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.minibus.hook.PTransitAgentFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;

import com.google.inject.Inject;

import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatchmentListener;
import playground.sebhoerl.avtaxi.schedule.AVOptimizer;

public class AVQSimProvider implements Provider<Mobsim> {
    @Inject private EventsManager eventsManager;
    @Inject private Collection<AbstractQSimPlugin> plugins;
    @Inject private Scenario scenario;

    @Inject private Injector injector;
    @Inject private AVConfig config;

    @Override
    public Mobsim get() {
    	List<AbstractQSimPlugin> plugins = new LinkedList<>();
    	
    	for (AbstractQSimPlugin plugin : this.plugins) {
    		if (!(plugin instanceof TransitEnginePlugin) && !(plugin instanceof PopulationPlugin)) {
    			plugins.add(plugin);
    		}
    	}
    	
        QSim qSim = QSimUtils.createQSim(scenario, eventsManager, plugins);
    	
        
    	/*QSim qSim = new QSim(scenario, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngineModule.configure(qSim);
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);*/
		
    	
        Injector childInjector = injector.createChildInjector(new AVQSimModule(config, qSim));

        qSim.addQueueSimulationListeners(childInjector.getInstance(AVOptimizer.class));
        qSim.addQueueSimulationListeners(childInjector.getInstance(AVDispatchmentListener.class));

        qSim.addMobsimEngine(childInjector.getInstance(PassengerEngine.class));
        qSim.addDepartureHandler(childInjector.getInstance(PassengerEngine.class));
        qSim.addAgentSource(childInjector.getInstance(VrpAgentSource.class));
        
        
        AgentFactory agentFactory;

		if (scenario.getConfig().transit().isUseTransit()) {
			agentFactory = new PTransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			agentFactory = new DefaultAgentFactory(qSim);
		}
		
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		
        return qSim;
    }
}
