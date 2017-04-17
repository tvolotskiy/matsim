package playground.manserpa.minibus;

import org.apache.log4j.Logger;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.scenario.MutableScenario;

public class PseudoReplanning  {

	private final static Logger log = Logger.getLogger(PseudoReplanning.class);
	private MatsimServices controler;
	
	
	public PseudoReplanning(MatsimServices controler)	{
		this.controler = controler;
		
		AgentReRouteHandlerImpl agentsToReRoute = new AgentReRouteHandlerImpl(this.controler.getScenario().getPopulation().getPersons());
		
		AgentReRouteFactoryImpl stuckFactory = new AgentReRouteFactoryImpl();
		
		ParallelPersonAlgorithmRunner.run(controler.getScenario().getPopulation(), controler.getConfig().global().getNumberOfThreads(), new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return stuckFactory.getReRouteStuck(new PlanRouter(
						controler.getTripRouterProvider().get(),
						controler.getScenario().getActivityFacilities()
						), ((MutableScenario) controler.getScenario()), agentsToReRoute.getAgentsToReRoute());
			}
		});
	
	}
}