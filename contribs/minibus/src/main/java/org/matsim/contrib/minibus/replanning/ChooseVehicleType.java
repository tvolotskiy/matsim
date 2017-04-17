package org.matsim.contrib.minibus.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConfigGroup.PVehicleSettings;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;


public final class ChooseVehicleType extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(ChooseVehicleType.class);
	public static final String STRATEGY_NAME = "ChooseVehicleType";
	private PConfigGroup pConfig;
	
	public ChooseVehicleType(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 0){
			log.error("No parameter needed here");
		}
	}

	@Override
	public PPlan run(Operator operator) {

		PPlan oldPlan = operator.getBestPlan();
		
		// calculate the timetable intervals of the old plan (maybe this can be easier done)
		Id<TransitRoute> routeId = Id.create(Id.create(operator.getId(), TransitLine.class) + "-" + Id.create(oldPlan.getId(), TransitRoute.class), TransitRoute.class);
		TransitRoute route = oldPlan.getLine().getRoutes().get(routeId);
		double numberOfVehiclesOld = oldPlan.getTotalPassengerKilometer() / oldPlan.getPassengerKilometerPerVehicle();
		double headway =  (this.pConfig.getDriverRestTime() + route.getStops().get(route.getStops().size() - 1).getDepartureOffset()) / numberOfVehiclesOld;
		double vehiclesPerHourOld = 3600 / headway;
		
		
		String pVehicleTypeOld = oldPlan.getPVehicleType();
		String pVehicleTypeNew = pVehicleTypeOld;
		
		// choose the desired vehicle type (may not be the best)
		while(pVehicleTypeOld.equals(pVehicleTypeNew))	{
			pVehicleTypeNew = operator.getRouteProvider().getRandomPVehicle();
		}
		
		// create new plan with the same route and operation time as the old plan and set the vehicle type to the new vehicle type
		PPlan newPlan = new PPlan(operator.getNewPlanId(), this.getStrategyName(), oldPlan.getId());
		
		newPlan.setStartTime(oldPlan.getStartTime());
		newPlan.setEndTime(oldPlan.getEndTime());
		newPlan.setStopsToBeServed(oldPlan.getStopsToBeServed());
		
		newPlan.setScore(0.0);
		newPlan.setPVehicleType(pVehicleTypeNew);
		
		
		// if the score is 0, the operator already changed the vehicle type and this should only happen once
		if(oldPlan.getScore() != 0)	{
		
			// get costs of old and new vehicle type
			double costsOld = 0.0;
			double costsNew = 0.0;
			double capacityOld = 0.0;
			double capacityNew = 0.0;
			double earningsOld = 0.0;
			double earningsNew = 0.0;
			
			// modify that according to the departure intervals
			double occupancy = oldPlan.getPassengerKilometerPerVehicle() / oldPlan.getTotalKilometersDrivenPerVehicle();
			
			
			// this is for the decision between old and new vehicle type
			double nVehiclesOld = oldPlan.getNVehicles();
			
			for (PVehicleSettings pVS : this.pConfig.getPVehicleSettings()) {
	            if (pVehicleTypeOld.equals(pVS.getPVehicleName())) {
	            	costsOld = pVS.getCostPerKilometer() * oldPlan.getTotalKilometersDrivenPerVehicle() + 
	            			pVS.getCostPerHour() * oldPlan.getTotalHoursDrivenPerVehicle();
	            	earningsOld = pVS.getEarningsPerKilometerAndPassenger() * oldPlan.getTotalKilometersDrivenPerVehicle();
	            	capacityOld = pVS.getCapacityPerVehicle();
	            }
	            if (pVehicleTypeNew.equals(pVS.getPVehicleName())) {
	            	costsNew = pVS.getCostPerKilometer() * oldPlan.getTotalKilometersDrivenPerVehicle() + 
	            			pVS.getCostPerHour() * oldPlan.getTotalHoursDrivenPerVehicle();
	            	earningsNew = pVS.getEarningsPerKilometerAndPassenger() * oldPlan.getTotalKilometersDrivenPerVehicle();
	            	capacityNew = pVS.getCapacityPerVehicle();
	            }
	        }
			
			double totalCostsOld = costsOld * nVehiclesOld;
			double totalCostsNew = 0.0;
			
			// Kapazität über Kosten ausgleichen. Der Betreiber soll nachher immer weniger bezahlen.
			// ab hier gilt die Entscheidung: setze ich beispielsweise einen Standardbus ein oder einen Minibus??
			do	{
				nVehiclesOld = nVehiclesOld - 1;
				totalCostsOld = costsOld * nVehiclesOld;
				
				newPlan.setNVehicles(newPlan.getNVehicles() + 1);
				totalCostsNew = costsNew * newPlan.getNVehicles();	
			} while (totalCostsNew < totalCostsOld);
			
			int nVehicles = 0;
			if (oldPlan.getNVehicles() == 0)	{
				nVehicles = 1;
			}
			else {
				nVehicles = oldPlan.getNVehicles();
			}
			
			double seatCostsOld = costsOld * nVehicles * capacityOld;
			double seatEarningsOld = earningsOld * nVehicles * capacityOld;
			
			double seatCostsNew = costsNew * newPlan.getNVehicles() * capacityNew;
			double seatEarningsNew = earningsNew * newPlan.getNVehicles() * capacityNew;
			
			// add time dependency and encourage the operators to operator longer 
			double timeFactor = 1.0;
			
			double operationTime = (oldPlan.getEndTime() - oldPlan.getStartTime()) / 3600;
			if (operationTime <= 3)	
				timeFactor = 1.0;
			else if(operationTime >= 12)
				timeFactor = 0.7;
			else
				timeFactor = -.04444444 * operationTime + 1.133333333; 
			
			double marginalOccupancy = timeFactor * (seatCostsNew - seatCostsOld) / (seatEarningsNew - seatEarningsOld);
			
			
			// calculation of the EXPECTED new occupancy 
			
			double headwayNew =  (this.pConfig.getDriverRestTime() + route.getStops().get(route.getStops().size() - 1).getDepartureOffset()) / newPlan.getNVehicles();
			double vehiclesPerHourNew = 3600 / headwayNew;
			
			double demandRatio = (2.2 * Math.log(vehiclesPerHourNew) + 1) / (2.2 * Math.log(vehiclesPerHourOld) + 1);
			
			// now I can calculate the expected passenger kilometers with the new Takt
			double expectedPaxKilometer = oldPlan.getTotalPassengerKilometer() * demandRatio;
			
			double occupancyNew = expectedPaxKilometer / (oldPlan.getTotalKilometersDrivenPerVehicle() * newPlan.getNVehicles());
			
			log.info(" ");
			log.info("Operator " + operator.getId() + " Old Plan " + oldPlan.getId());
			log.info("Old vehicle type " + pVehicleTypeOld + " number of vehicles: " +nVehicles);
			log.info("New vehicle type " + pVehicleTypeNew + " number of vehicles: " +newPlan.getNVehicles());
			log.info("Marginal Occupancy " + marginalOccupancy);
			log.info("Occupancy " + occupancy);
			log.info("Excp. Occupancy " + occupancyNew);
			log.info("Vehicles Per Hour Old " + vehiclesPerHourOld);
			log.info("PaxKilometer Old " + oldPlan.getTotalPassengerKilometer());
			log.info("Vehicles Per Hour New " + vehiclesPerHourNew);
			log.info("PaxKilometer Expected " + expectedPaxKilometer);
			
			double deltaOccupancy = occupancyNew - marginalOccupancy;
			
			// Unterscheiden zwischen Up-/Downgrade
			if(capacityOld < capacityNew)	{
				//it's an upgrade
				if (doChangeVehicleType(deltaOccupancy, true, false))	{
					// reset the old plan
					log.info("Will change: true");
					oldPlan.setNVehicles(0);
					oldPlan.setScore(0.0);
					
					newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
					return newPlan;
				}
			}
			if(capacityOld > capacityNew)	{
				//it's a downgrade
				if (doChangeVehicleType((-1 * deltaOccupancy), false, true))	{
					// reset the old plan
					log.info("Will change: true");
					oldPlan.setNVehicles(0);
					oldPlan.setScore(0.0);
					
					newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
					return newPlan;
				}
			}
		}
		
		newPlan.setNVehicles(0);
		return newPlan;
		
	}
	
	private boolean doChangeVehicleType( double deltaOccupancy, boolean isUpgrade, boolean isDowngrade ) {
		
		double probabilityToChange = 0.0;		
		
		if (isUpgrade)
			probabilityToChange = 1 / ( 1 + 5 * Math.exp(-deltaOccupancy / 2));
		else if (isDowngrade)
			probabilityToChange = 1 / ( 1 + 15 * Math.exp(-deltaOccupancy / 2.2));
		
		log.info("Probability to change: " + probabilityToChange);
			
		double rndTreshold = MatsimRandom.getRandom().nextDouble();
		log.info("Treshold: " + rndTreshold);
		if(probabilityToChange > rndTreshold)	{
			return true;
		}
		
		return false;
	}
	
		/*
		double occupancy = oldPlan.getTotalPassengerKilometer() * 1000 / oldPlan.getTotalMeterDriven();
		
		// now calculate the occupancy needed to the other vehicle types
		double costsOld = 0.0;
		double revenueOld = 0.0;
		int capacityOld = 0;
		
		
		
		for (PVehicleSettings pVS : this.pConfig.getPVehicleSettings()) {
            if (pVehicleTypeOld.equals(pVS.getPVehicleName())) {
            	costsOld = pVS.getCapacityPerVehicle() * (pVS.getCostPerKilometer() * oldPlan.getTotalMeterDriven() / 1000 + 
            			pVS.getCostPerHour() * oldPlan.getTotalTimeDriven() / 3600);
            	revenueOld = pVS.getCapacityPerVehicle() * pVS.getEarningsPerKilometerAndPassenger() * oldPlan.getTotalMeterDriven() / 1000;
            	capacityOld = pVS.getCapacityPerVehicle();
            }
        }
		
		
		
		double maxDeltaOccupancyNeg = 0.0;
		double maxDeltaOccupancyPos = 0.0;
		String vehicleTypeNewNeg = null;
		String vehicleTypeNewPos = null;
		
		
		// if the old plan does not have a score, it is not possible to change the vehicle type
		if(oldPlan.getScore() != 0)	{
			
			for (PVehicleSettings pVS : this.pConfig.getPVehicleSettings()) {
	            if (!pVehicleTypeOld.equals(pVS.getPVehicleName())) {
	            	
	            	double costsNew = pVS.getCapacityPerVehicle() * (pVS.getCostPerKilometer() * oldPlan.getTotalMeterDriven() / 1000 + 
                			pVS.getCostPerHour() * oldPlan.getTotalTimeDriven() / 3600);
            		double revenueNew = pVS.getCapacityPerVehicle() * pVS.getEarningsPerKilometerAndPassenger() * oldPlan.getTotalMeterDriven() / 1000;
            		
            		double marginalOccupancy = (costsNew - costsOld) / (revenueNew - revenueOld);
            		
            		double deltaOccupancy = occupancy - marginalOccupancy;
            		
	            	// should the operator use smaller vehicles?
	            	if(pVS.getCapacityPerVehicle() < capacityOld && deltaOccupancy < 0)	{
	            		
	            		if ( deltaOccupancy < maxDeltaOccupancyNeg )	{
	            			maxDeltaOccupancyNeg = deltaOccupancy;
	            			vehicleTypeNewNeg = pVS.getPVehicleName();
	            		}

	            	}
	            	// should the operator use bigger vehicles?
	            	if(pVS.getCapacityPerVehicle() > capacityOld && deltaOccupancy > 0)	{
	            		
	            		if ( deltaOccupancy > maxDeltaOccupancyPos )	{
	            			maxDeltaOccupancyPos = deltaOccupancy;
	            			vehicleTypeNewPos = pVS.getPVehicleName();
	            		}
	            		
	            	}
	            }
	        }
			
			double maxDeltaOccupancy = 0.0;
			
			if (maxDeltaOccupancyPos > Math.abs(maxDeltaOccupancyNeg))	{
				maxDeltaOccupancy = maxDeltaOccupancyPos;
				newPlan.setPVehicleType(vehicleTypeNewPos);
			}
			else if(maxDeltaOccupancyPos < Math.abs(maxDeltaOccupancyNeg))	{
				maxDeltaOccupancy = maxDeltaOccupancyNeg;
				newPlan.setPVehicleType(vehicleTypeNewNeg);
			}
			
			if(doChangeVehicleType(maxDeltaOccupancy))	{
    			newPlan.setNVehicles(oldPlan.getNVehicles());
    			
    			oldPlan.setNVehicles(0);
    			oldPlan.setScore(0.0);
    			
    			newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
    			return newPlan;
    		}
		}
		
		else	{
			
			newPlan.setNVehicles(0);
			
			return newPlan;
			
		}
		
		newPlan.setNVehicles(0);
		
		return newPlan;
		
	}
	
	public boolean doChangeVehicleType( double deltaOccupancy ) {
		
		// probability to change
		double probToChange = 0.1 * deltaOccupancy;
			
		double rndTreshold = MatsimRandom.getRandom().nextDouble();
		
		if(probToChange > rndTreshold)	{
			return true;
		}
		
		return false;
	}
		
		*/
		
		
		/*
		
		
		// get costs of old and new vehicle type
		double costsOld = 0.0;
		double costsNew = 0.0;
		
		for (PVehicleSettings pVS : this.pConfig.getPVehicleSettings()) {
            if (pVehicleTypeOld.equals(pVS.getPVehicleName())) {
            	costsOld = pVS.getCostPerKilometer() + pVS.getCostPerHour();
            }
            if (pVehicleTypeNew.equals(pVS.getPVehicleName())) {
            	costsNew = pVS.getCostPerKilometer() + pVS.getCostPerHour();
            }
        }
		
		// if the old plan does not have a score, it is not possible to change the vehicle type
		if(oldPlan.getScore() != 0)	{
		
			// if more than one vehicles exist in the plan, one need to balance the capacities via costs
			if(oldPlan.getNVehicles() > 1)	{
				
				// the new vehicles are smaller than the old ones
				if (costsOld > costsNew)	{
					// this does not make any sense, because the operator can first sell the big vehicles and try it with less, but still big
					// vehicles -> good as it is
					
					// TODO: check this part
					newPlan.setNVehicles(0);
					//newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));

				}
				
				
				// if the new vehicles are bigger than the old ones, no vehicle is added, but the operator has sort of
				// to pay for the capacity upgrade				
				else	{
					
					// TODO: Still not sure if everything works fine in here
					
					// the operator upgrades only if the old plan has a positive score and the probability of upgrading increases with 
					// increasing score
//					if(oldPlan.getScore() > 0 && doUpgrade(oldPlan.getScore()))	{
						
						double totalCostsOld = costsOld * oldPlan.getNVehicles();
						double totalCostsNew = 0.0;
						
						while (totalCostsNew < totalCostsOld)	{
							oldPlan.setNVehicles(oldPlan.getNVehicles() - 1);
							totalCostsOld = costsOld * oldPlan.getNVehicles();
							
							newPlan.setNVehicles(newPlan.getNVehicles() + 1);
							totalCostsNew = costsNew * newPlan.getNVehicles();	
						}
						
						//if (newPlan.getNVehicles() > 1)	{
						//	oldPlan.setNVehicles(oldPlan.getNVehicles() + 1);
						//	newPlan.setNVehicles(newPlan.getNVehicles() - 1);
						//}
						
						// reset the old plan
						oldPlan.setNVehicles(0);
						oldPlan.setScore(0.0);
						
						newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
						
						//return newPlan;
//					}
					
//					else	{
						
//						newPlan.setNVehicles(0);
						//return newPlan;
					
//					}
				}
			}
			
			// if only one vehicle exist in the plan, only one vehicle of the new type depending on the score
			else	{
				
				if (costsOld > costsNew)	{
					if (oldPlan.getScore() < 0 && doDowngrade( oldPlan.getScore() ))	{
						newPlan.setNVehicles(1);
						newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
						
						oldPlan.setNVehicles(0);
						oldPlan.setScore(0.0);
		
					}
					else	{
						newPlan.setNVehicles(0);
					}
				}
				
				// new vehicle is bigger than the old one
				else	{
					if (oldPlan.getScore() > 0 && doUpgrade( oldPlan.getScore() ))	{
						newPlan.setNVehicles(1);
						newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
						
						oldPlan.setNVehicles(0);
						oldPlan.setScore(0.0);

					}
					else	{
						newPlan.setNVehicles(0);
					}
				}
			}
		}
		else	{
			newPlan.setNVehicles(0);
		}
		
		return newPlan;
	}
	
	public boolean doDowngrade( double score ) {
		
		score = Math.abs(score);
		
		// if the score is very low, do downgrade anyway
		if (score > 1000)	{
			return true;
		}
		
		double scaleparameter = 1;
			
		// probability to change
		double probToChange = scaleparameter / Math.pow(500, 2) * Math.pow(score, 2);
			
		double rndTreshold = MatsimRandom.getRandom().nextDouble();
		
		if(probToChange > rndTreshold)	{
			return true;
		}
		
		return false;
	}
	
	public boolean doUpgrade( double score ) {
		
		// if the score is very high, do update anyway
		if (score > 2000)	{
			return true;
		}
		
		double scaleparameter = 0.8;
			
		// probability to change
		double probToChange = scaleparameter / Math.pow(1000, 2) * Math.pow(score, 2);
			
		double rndTreshold = MatsimRandom.getRandom().nextDouble();
		
		if(probToChange > rndTreshold)	{
			return true;
		}
		
		return false;
	}
	*/
		
		
	public void setPConfig(PConfigGroup pConfig) {
		this.pConfig = pConfig;
	}
	
	public String getStrategyName() {
		return ChooseVehicleType.STRATEGY_NAME;
	}
}