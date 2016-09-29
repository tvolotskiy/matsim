package org.matsim.core.mobsim.qsim.qnetsimengine.assignment;

import com.google.inject.ImplementedBy;

@ImplementedBy(RoundRobinNodeAssignmentFactory.class)
public interface QNetsimNodeAssignmentFactory {
	QNetsimNodeAssignment createNodeAssignment(int numberOfRunners);
}
