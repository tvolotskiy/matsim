package playground.sebhoerl.decomposition;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.assignment.QNetsimNodeAssignment;

public class StaticNodeAssignment implements QNetsimNodeAssignment {
	final private Map<Id<Node>, Integer> lookup;
	
	public StaticNodeAssignment(Map<Id<Node>, Integer> lookup) {
		this.lookup = lookup;
	}
	
	@Override
	public int findEngine(Id<Node> nodeId) {
		Integer result = lookup.get(nodeId);
		
		if (result == null) {
			throw new IllegalArgumentException("Node " + nodeId.toString() + " is not registered.");
		}
		
		return result;
	}

}
