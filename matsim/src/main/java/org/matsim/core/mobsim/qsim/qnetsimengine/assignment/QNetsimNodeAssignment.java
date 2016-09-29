package org.matsim.core.mobsim.qsim.qnetsimengine.assignment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public interface QNetsimNodeAssignment {
	int findEngine(Id<Node> nodeId);
}
