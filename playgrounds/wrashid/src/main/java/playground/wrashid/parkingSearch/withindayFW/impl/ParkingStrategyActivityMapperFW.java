/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,     *
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

package playground.wrashid.parkingSearch.withindayFW.impl;

import java.util.Collection;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingStrategyActivityMapper;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategyFW.ParkingStrategy;

public class ParkingStrategyActivityMapperFW implements ParkingStrategyActivityMapper {

	HashMap<Id, LinkedListValueHashMap<String, ParkingStrategy>> mapping;

	public ParkingStrategyActivityMapperFW() {
		mapping = new HashMap<Id, LinkedListValueHashMap<String, ParkingStrategy>>();
	}

	@Override
	public Collection<ParkingStrategy> getParkingStrategies(Id agentId, String activityType) {
		return mapping.get(agentId).get(activityType);
	}

	@Override
	public void addSearchStrategy(Id agentId, String activityType, ParkingStrategy parkingStrategy) {
		if (!mapping.containsKey(agentId)) {
			mapping.put(agentId, new LinkedListValueHashMap<String, ParkingStrategy>());
		}

		LinkedListValueHashMap<String, ParkingStrategy> linkedListValueHashMap = mapping.get(agentId);

		linkedListValueHashMap.put(activityType, parkingStrategy);
	}

}
