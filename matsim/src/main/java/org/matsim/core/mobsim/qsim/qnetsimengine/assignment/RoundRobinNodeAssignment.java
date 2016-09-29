package org.matsim.core.mobsim.qsim.qnetsimengine.assignment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class RoundRobinNodeAssignment implements QNetsimNodeAssignment {
	final int numberOfRunners;
	int roundRobin = 0;
	
	public RoundRobinNodeAssignment(int numberOfRunners) {
		this.numberOfRunners = numberOfRunners;
	}
	
	@Override
	public int findEngine(Id<Node> nodeId) {
		return (roundRobin++) % this.numberOfRunners;
	}
}
