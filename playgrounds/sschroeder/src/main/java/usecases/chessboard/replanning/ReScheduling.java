package usecases.chessboard.replanning;

import java.util.Collection;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.router.util.TravelTime;

public class ReScheduling implements GenericPlanStrategyModule<CarrierPlan>{
	
	private final Network network;

	private final CarrierVehicleTypes vehicleTypes;

	private final TravelTime travelTimes;
	
	private final String vrpAlgorithmConfig;

	public ReScheduling(Network network, CarrierVehicleTypes vehicleTypes, TravelTime travelTimes, String vrpAlgoConfigFile) {
		super();
		this.network = network;
		this.vehicleTypes = vehicleTypes;
		this.travelTimes = travelTimes;
		this.vrpAlgorithmConfig = vrpAlgoConfigFile;
	}
	
	@Override
	public void handlePlan(CarrierPlan carrierPlan) {
		//		System.out.println("REPLAN " + carrierPlan.getCarrier().getId());
		Carrier carrier = carrierPlan.getCarrier();

		//construct the routing problem - here the interface to jsprit comes into play
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);

		//******
		//Define transport-costs
		//******
		//construct network-based routing costs
		//by default travelTimes are calculated with freeSpeed and vehicleType.maxVelocity on the network
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, vehicleTypes.getVehicleTypes().values());
		//sets time-dependent travelTimes
		tpcostsBuilder.setTravelTime(travelTimes);
		//sets time-slice to build time-dependent tpcosts and traveltime matrices
		tpcostsBuilder.setTimeSliceWidth(900);

		//assign netBasedCosts to RoutingProblem
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();

		//set transport-costs
		vrpBuilder.setRoutingCost(netbasedTransportcosts);

		//******
		//Define activity-costs
		//******
		//should be inline with activity-scoring 
//		VehicleRoutingActivityCosts activitycosts = new VehicleRoutingActivityCosts(){
//
//			private double penalty4missedTws = 0.008; 
//
//			@Override
//			public double getActivityCost(TourActivity act, double arrivalTime, Driver arg2, Vehicle vehicle) {	
//				double tooLate = Math.max(0, arrivalTime - act.getTheoreticalLatestOperationStartTime());
//				//				double waiting = Math.max(0, act.getTheoreticalEarliestOperationStartTime() - arrivalTime);
//				//				double service = act.getOperationTime()*vehicle.getType().getVehicleCostParams().perTimeUnit;
//				//				return penalty4missedTws*tooLate + vehicle.getType().getVehicleCostParams().perTimeUnit*waiting + service;
//				//				return penalty4missedTws*tooLate;
//				return 0.0;
//			}
//
//		};
//		vrpBuilder.setActivityCosts(activitycosts);

		//build the problem
		VehicleRoutingProblem vrp = vrpBuilder.build();

		//get configures algorithm
		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, vrpAlgorithmConfig);
		//		vra.getAlgorithmListeners().addListener(new AlgorithmSearchProgressChartListener("output/"+carrierPlan.getCarrier().getId() + "_" + carrierPlan.hashCode() + ".png"));
		//add initial-solution - which is the initialSolution for the vehicle-routing-algo
		vra.addInitialSolution(MatsimJspritFactory.createSolution(carrierPlan, network));

		//solve problem
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();

		//get best 
		VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);

		//		SolutionPlotter.plotSolutionAsPNG(vrp, solution, "output/sol_"+System.currentTimeMillis()+".png", "sol");

		//create carrierPlan from solution
		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);

		//route plan (currently jsprit does not memorizes the routes, thus route the plan)
//		NetworkRouter.routePlan(plan, netbasedTransportcosts);

		//set new plan
		carrierPlan.getScheduledTours().clear();
		carrierPlan.getScheduledTours().addAll(plan.getScheduledTours());

	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub

	}

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub

	}
	
}
