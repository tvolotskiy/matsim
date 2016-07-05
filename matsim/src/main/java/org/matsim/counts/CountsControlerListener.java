/* *********************************************************************** *
 * project: org.matsim.*
 * CountControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.counts;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsHtmlAndGraphsWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author dgrether
 */
class CountsControlerListener implements StartupListener, IterationEndsListener {

	/*
	 * String used to identify the operation in the IterationStopWatch.
	 */
	public static final String OPERATION_COMPARECOUNTS = "compare with counts";

    private GlobalConfigGroup globalConfigGroup;
    private Network network;
    private ControlerConfigGroup controlerConfigGroup;
    private final CountsConfigGroup config;
    private final Set<String> analyzedModes;
    private final VolumesAnalyzer volumesAnalyzer;
    private final IterationStopWatch iterationStopwatch;
    private final OutputDirectoryHierarchy controlerIO;

    @com.google.inject.Inject(optional=true)
    private Counts<Link> counts = null;

    private final Map<Id<Link>, Map<String,double[]>> linkStats = new HashMap<>();
    private int iterationsUsed = 0;

    @Inject
    CountsControlerListener(GlobalConfigGroup globalConfigGroup, Network network, ControlerConfigGroup controlerConfigGroup, CountsConfigGroup countsConfigGroup, VolumesAnalyzer volumesAnalyzer, IterationStopWatch iterationStopwatch, OutputDirectoryHierarchy controlerIO) {
        this.globalConfigGroup = globalConfigGroup;
        this.network = network;
        this.controlerConfigGroup = controlerConfigGroup;
        this.config = countsConfigGroup;
        this.volumesAnalyzer = volumesAnalyzer;
		this.analyzedModes = CollectionUtils.stringToSet(this.config.getAnalyzedModes());
        this.iterationStopwatch = iterationStopwatch;
        this.controlerIO = controlerIO;
	}

	@Override
	public void notifyStartup(final StartupEvent controlerStartupEvent) {
        if (counts != null) {
            for (Id<Link> linkId : counts.getCounts().keySet()) {
                Map<String, double[]> mode2counts = new HashMap<>();
                if(this.config.isFilterModes()){
                for(String mode : this.analyzedModes){
                	mode2counts.put(mode, new double [24]);
                }
                } else {// adding a fake mode 
                	mode2counts.put("all", new double [24]);
                }
            	this.linkStats.put(linkId, mode2counts);
            }
        }
	}

    @Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (counts != null && this.config.getWriteCountsInterval() > 0) {
            if (useVolumesOfIteration(event.getIteration(), controlerConfigGroup.getFirstIteration())) {
                addVolumes(volumesAnalyzer);
            }

            if (createCountsInIteration(event.getIteration())) {
                iterationStopwatch.beginOperation(OPERATION_COMPARECOUNTS);
                Map<Id<Link>, Map<String, double[]>> averages;
                if (this.iterationsUsed > 1) {
                    averages = new HashMap<>();
                    for (Map.Entry<Id<Link>, Map<String, double[]>> e : this.linkStats.entrySet()) {
                        Id<Link> linkId = e.getKey();
                        Map<String, double[]> mode2avgcounts = new HashMap<>();
                        for(Entry<String, double[]> entry : e.getValue().entrySet()) {
                        	double[] totalVolumesPerHour = entry.getValue();
                        	double[] averageVolumesPerHour = new double[totalVolumesPerHour.length];
                        	for (int i = 0; i < totalVolumesPerHour.length; i++) {
                                averageVolumesPerHour[i] = totalVolumesPerHour[i] / this.iterationsUsed;
                            }
                        	mode2avgcounts.put(entry.getKey(), averageVolumesPerHour);
                        }
                        averages.put(linkId, mode2avgcounts);
                    }
                } else {
                    averages = this.linkStats;
                }
//                CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(averages, counts, network, config.getCountsScaleFactor());
//                if ((this.config.getDistanceFilter() != null) && (this.config.getDistanceFilterCenterNode() != null)) {
//                    cca.setDistanceFilter(this.config.getDistanceFilter(), this.config.getDistanceFilterCenterNode());
//                }
//                cca.setCountsScaleFactor(this.config.getCountsScaleFactor());
//                cca.run();

//                if (this.config.getOutputFormat().contains("html") ||
//                        this.config.getOutputFormat().contains("all")) {
//                    CountsHtmlAndGraphsWriter cgw = new CountsHtmlAndGraphsWriter(controlerIO.getIterationPath(event.getIteration()), cca.getComparison(), event.getIteration());
//                    cgw.addGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
//                    cgw.addGraphsCreator(new CountsErrorGraphCreator("errors"));
//                    cgw.addGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
//                    cgw.addGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
//                    cgw.createHtmlAndGraphs();
//                }
//                if (this.config.getOutputFormat().contains("kml") ||
//                        this.config.getOutputFormat().contains("all")) {
//                    String filename = controlerIO.getIterationFilename(event.getIteration(), "countscompare.kmz");
//                    CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
//                            cca.getComparison(), network, TransformationFactory.getCoordinateTransformation(globalConfigGroup.getCoordinateSystem(), TransformationFactory.WGS84));
//                    kmlWriter.setIterationNumber(event.getIteration());
//                    kmlWriter.writeFile(filename);
//                }
//                if (this.config.getOutputFormat().contains("txt") ||
//                        this.config.getOutputFormat().contains("all")) {
//                    String filename = controlerIO.getIterationFilename(event.getIteration(), "countscompare.txt");
//                    CountSimComparisonTableWriter ctw = new CountSimComparisonTableWriter(cca.getComparison(), Locale.ENGLISH);
//                    ctw.writeFile(filename);
//                }
                reset();
                iterationStopwatch.endOperation(OPERATION_COMPARECOUNTS);
            }
        }
	}

	/*package*/ boolean useVolumesOfIteration(final int iteration, final int firstIteration) {
		int iterationMod = iteration % this.config.getWriteCountsInterval();
		int effectiveIteration = iteration - firstIteration;
		int averaging = Math.min(this.config.getAverageCountsOverIterations(), this.config.getWriteCountsInterval());
		if (iterationMod == 0) {
			return ((this.config.getAverageCountsOverIterations() <= 1) ||
					(effectiveIteration >= averaging));
		}
		return (iterationMod > (this.config.getWriteCountsInterval() - this.config.getAverageCountsOverIterations())
				&& (effectiveIteration + (this.config.getWriteCountsInterval() - iterationMod) >= averaging));
	}
	
	/*package*/ boolean createCountsInIteration(final int iteration) {
		return ((iteration % this.config.getWriteCountsInterval() == 0) && (this.iterationsUsed >= this.config.getAverageCountsOverIterations()));		
	}

	private void addVolumes(final VolumesAnalyzer volumes) {
		this.iterationsUsed++;
		for (Map.Entry<Id<Link>, Map<String, double[]>> e : this.linkStats.entrySet()) {
			Id<Link> linkId = e.getKey();
			Map<String, double[]> mode2counts =  e.getValue();
			if( ! this.config.isFilterModes() ) {
				double[] volumesPerHour = mode2counts.values().iterator().next();//only one entry
				double[] newVolume = volumes.getVolumesPerHourForLink(linkId);	
				for (int i = 0; i < 24; i++) {
					volumesPerHour[i] += newVolume[i];
				}
				mode2counts.put(mode2counts.keySet().iterator().next(), volumesPerHour);
			} else {
				for (String mode : mode2counts.keySet()) {
					double[] volumesPerHour = mode2counts.get(mode); 
					double[] newVolume = volumes.getVolumesPerHourForLink(linkId, mode); 
					for (int i = 0; i < 24; i++) {
						volumesPerHour[i] += newVolume[i];
					}
					mode2counts.put(mode, volumesPerHour);
				}
				this.linkStats.put(linkId, mode2counts);	
			}
			
		}
	}
	
	private void reset() {
		this.iterationsUsed = 0;
		for(Map<String, double[]> mode2counts : this.linkStats.values()) {
			for (double[] hours : mode2counts.values()) {
				for (int i = 0; i < hours.length; i++) {
					hours[i] = 0.0;
				}
			}
		}
	}

}
