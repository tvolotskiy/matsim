package org.matsim.core.mobsim.qsim.qnetsimengine.assignment;

public class RoundRobinNodeAssignmentFactory implements QNetsimNodeAssignmentFactory {
	@Override
	public QNetsimNodeAssignment createNodeAssignment(int numberOfRunners) {
		return new RoundRobinNodeAssignment(numberOfRunners);
	}
}
