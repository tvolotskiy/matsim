/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.holes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import playground.agarwalamit.mixedTraffic.snapshot.MyPositionSnapShotWriter;

import org.matsim.vis.otfvis.OnTheFlyServer;

/**
 * @author amit
 */
public class HolesControlerExample {

	private static final boolean IS_USING_OTFVIS = false;
	private static final boolean IS_WRITING_FILES = false;
	private final static String outputDir = "./output/";

	public static void main(String[] args) {

		SimpleNetwork net = new SimpleNetwork();

		final Scenario sc = net.scenario;
		
		for (int i=0;i<20;i++){
			Id<Person> id = Id.createPersonId(i);
			Person p = net.population.getFactory().createPerson(id);
			Plan plan = net.population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = net.population.getFactory().createActivityFromLinkId("h", net.link1.getId());
			Leg leg  = net.population.getFactory().createLeg(TransportMode.car);
			a1.setEndTime(948+i*2);

			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link4.getId());
			route.setLinkIds( net.link1.getId(), 
							Arrays.asList(net.link2.getId(), net.link3.getId()), 
							net.link4.getId() );
			leg.setRoute(route);
			Activity a2 = net.population.getFactory().createActivityFromLinkId("w", net.link4.getId());
			plan.addActivity(a2);
			net.population.addPerson(p);
		}

		EventsManager manager = EventsUtils.createEventsManager();
		EventWriterXML 	ew;
		
		if(IS_WRITING_FILES){
			ew = new EventWriterXML("./output/events.xml");	
			new ConfigWriter(sc.getConfig()).write("./output/config.xml");
			new NetworkWriter(sc.getNetwork()).write("./output/network.xml");
			manager.addHandler(ew);
		}

		final Netsim qSim = QSimUtils.createDefaultQSim(sc, manager);

		if ( IS_USING_OTFVIS ) {
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			otfVisConfig.setColoringScheme(ColoringScheme.byId);
			//				otfVisConfig.setShowParking(true) ; // this does not really work

			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, manager, (QSim) qSim);
			OTFClientLive.run(sc.getConfig(), server);
		} else {
			sc.getConfig().qsim().setLinkWidthForVis((float)0);
			((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(0.);	
		}
		
		sc.getConfig().controler().setOutputDirectory(outputDir);
		sc.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		sc.getConfig().controler().setLastIteration(0);
		sc.getConfig().controler().setDumpDataAtEnd(false);
		sc.getConfig().controler().setCreateGraphs(false);
		
		Controler controler = new Controler (sc);
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				this.bindMobsim().toInstance(qSim);
				if(! IS_USING_OTFVIS) this.addSnapshotWriterBinding().to(MyPositionSnapShotWriter.class);
			}
		});
		
		controler.run();
		
		if(IS_WRITING_FILES) ew.closeFile();
	}

	private static final class SimpleNetwork{

		final Config config;
		final Scenario scenario ;
		final NetworkImpl network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;
		final Link link4;

		public SimpleNetwork(){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);
			
			config.qsim().setStuckTime(24*3600); // in order to let agents wait instead of forced entry.
			config.qsim().setEndTime(01*3600);

			config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);
			config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
			config.qsim().setSnapshotPeriod(1);

			network = (NetworkImpl) scenario.getNetwork();

			Node node1 = network.createAndAddNode(Id.createNodeId("1"), new Coord(-500., -500.0));
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), new Coord(0.0, 0.0));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), new Coord(1000.0, 0.0));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), new Coord(1500.0, 0.0));
			Node node5 = network.createAndAddNode(Id.createNodeId("5"), new Coord(1750.0, -500.0));

			Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList(TransportMode.car,TransportMode.walk));

			link1 = network.createAndAddLink(Id.createLinkId("1"), node1, node2, 707.11, 25, 3600, 1, null, "22"); 
			link2 = network.createAndAddLink(Id.createLinkId("2"), node2, node3, 1000, 25, 3600, 1, null, "22");	//flow capacity is 1 PCU per min.
			link3 = network.createAndAddLink(Id.createLinkId("3"), node3, node4, 500, 25, 360, 1, null, "22");
			link4 = network.createAndAddLink(Id.createLinkId("4"), node4, node5, 559.02, 25, 3600, 1, null, "22");

			population = scenario.getPopulation();
		}
	}
}