/* *********************************************************************** *
 * project: org.matsim.*
 * FastDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.DijkstraNodeDataFactory;
import org.matsim.core.router.util.RoutingNetworkFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;

/**
 * <p>
 * Performance optimized version of the Dijkstra {@link org.matsim.core.router.Dijkstra} 
 * least cost path router which uses its own network to route within. Note that 
 * this network requires additional memory for each FastDijkstra instance.
 * </p>
 * 
 * @see org.matsim.core.router.Dijkstra
 * @see org.matsim.core.router.util.RoutingNetwork
 * @author cdobler
 */
public class FastDijkstra extends Dijkstra {

	private final RoutingNetwork routingNetwork;
	private final FastRouterDelegate fastRouter;
	
	public FastDijkstra(final Network network, final TravelCost costFunction, final TravelTime timeFunction) {
		this(network, costFunction, timeFunction, null);
	}

	/*
	 * Create the routing network here and clear the nodeData map 
	 * which is not used by this implementation.
	 */
	public FastDijkstra(final Network network, final TravelCost costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData) {
		super(network, costFunction, timeFunction, preProcessData);
		
		this.routingNetwork = new RoutingNetworkFactory().createRoutingNetwork(network);
		this.routingNetwork.setPreProcessDijkstra(preProcessData);
		this.nodeData.clear();
				
		this.fastRouter = new FastRouterDelegate(this, new DijkstraNodeDataFactory());
	}
		
	/*
	 * Replace the references to the from and to nodes with their corresponding
	 * nodes in the routing network.
	 */
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {
		
		this.routingNetwork.initialize();
		
		RoutingNetworkNode routingNetworkFromNode = routingNetwork.getNodes().get(fromNode.getId());
		RoutingNetworkNode routingNetworkToNode = routingNetwork.getNodes().get(toNode.getId());

		return super.calcLeastCostPath(routingNetworkFromNode, routingNetworkToNode, startTime);
	}
	
	/*
	 * Constructs the path and replaces the nodes and links from the routing network
	 * with their corresponding nodes and links from the network.
	 */
	@Override
	protected Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
		return this.fastRouter.constructPath(fromNode, toNode, startTime, arrivalTime);
	}
	
	/*
	 * For performance reasons the outgoing links of a node are stored in
	 * the routing network in an array instead of a map. Therefore we have
	 * to iterate over an array instead of over a map. 
	 */
	@Override
	protected void relaxNode(final Node outNode, final Node toNode, final PseudoRemovePriorityQueue<Node> pendingNodes) {
		fastRouter.relaxNode(outNode, toNode, pendingNodes);
	}
	
	/*
	 * The DijkstraNodeData is taken from the RoutingNetworkNode and not from a map.
	 */
	@Override
	protected DijkstraNodeData getData(final Node n) {
		return (DijkstraNodeData) fastRouter.getData(n);
	}

	/*
	 * The DeadEndData is taken from the RoutingNetworkNode and not from a map.
	 */
	@Override
	protected PreProcessDijkstra.DeadEndData getPreProcessData(final Node n) {
		return fastRouter.getPreProcessData(n);
	}
}
