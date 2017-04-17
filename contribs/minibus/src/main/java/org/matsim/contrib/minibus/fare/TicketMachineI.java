package org.matsim.contrib.minibus.fare;

public interface TicketMachineI {

	double getFare(StageContainer stageContainer);
	
	double getPassengerDistanceKilometer(StageContainer stageContainer);

}