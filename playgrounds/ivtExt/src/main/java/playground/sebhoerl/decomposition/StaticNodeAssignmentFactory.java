package playground.sebhoerl.decomposition;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.assignment.QNetsimNodeAssignment;
import org.matsim.core.mobsim.qsim.qnetsimengine.assignment.QNetsimNodeAssignmentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.assignment.RoundRobinNodeAssignment;

public class StaticNodeAssignmentFactory implements QNetsimNodeAssignmentFactory {
	final private Map<Id<Node>, Integer> lookup = new HashMap<>();
	private int maximumEngineIndex = 0;
	
	@Override
	public QNetsimNodeAssignment createNodeAssignment(int numberOfRunners) {
		if (maximumEngineIndex >= numberOfRunners) {
			throw new RuntimeException("Too many QNetsimEngineRunner assignments registered");
		}
		
		return new StaticNodeAssignment(lookup);
	}
	
	void registerAssignment(Id<Node> nodeId, int engine) {
		maximumEngineIndex = Math.max(maximumEngineIndex, engine);
		lookup.put(nodeId, engine);
	}
}
