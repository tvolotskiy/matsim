package playground.sebhoerl.decomposition;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.assignment.QNetsimNodeAssignmentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.assignment.RoundRobinNodeAssignmentFactory;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;

public class RunQSimBenchmark {
	final private Config config;
	final private Scenario scenario;
	final private BenchmarkEventsManager events;
	
	static private String configPath;
	static private String csvPath;
	static private String outputPath;
	
	public static void main(String[] args) throws IOException {
		configPath = args[0]; // /home/sebastian/sioux-2016/config.xml
		csvPath = args[1]; // /home/sebastian/decomposition/sioux.csv
		outputPath = args[2]; // /home/sebastian/decomposition/output.json
		
		new RunQSimBenchmark().run();
	}
	
	public RunQSimBenchmark() {
		Logger.getRootLogger().removeAllAppenders();
		System.out.println("Loading ... ");
		
		config = ConfigUtils.loadConfig(configPath);
		scenario = ScenarioUtils.loadScenario(config);
		events = new BenchmarkEventsManager();
	}
	
	void run() throws IOException {
		long start, end;
		
		// Do some dry runs to initialize all the IDs
		for (int i = 1; i < 5; i++) {
			System.out.println("Running warmup iteration " + i);
			events.reset();
			
			QSim qsim = createQSimForDecomposition(4, false);
			
			start = System.nanoTime();
			qsim.run();
			end = System.nanoTime();
			
			System.out.println("Took " + (end - start) + "ns");
			events.printCounts();
		}
		
		Map<Integer, List<Long>> roundRobinResults = new HashMap<>();
		Map<Integer, List<Long>> decompositionResults = new HashMap<>();
		
		// Test each case 10 times (5x dec, 5x RR)
		for (int n = 2; n < 25; n++) {
			ArrayList<Long> localDecompositionResults = new ArrayList<>(5);
			ArrayList<Long> localRoundRobinResults = new ArrayList<>(5);
			
			for (int i = 0; i < 5; i++) {
				System.out.println("Running decomposition iteration " + i + " for n=" + n);
				events.reset();
				
				QSim qsim = createQSimForDecomposition(n, true);
				
				start = System.nanoTime();
				qsim.run();
				end = System.nanoTime();

				localDecompositionResults.add(end - start);
				
				System.out.println("Took " + (end - start) + "ns");
				events.printCounts();
			}

			for (int i = 0; i < 5; i++) {
				System.out.println("Running RR iteration " + i + " for n=" + n);
				events.reset();

				QSim qsim = createQSimForDecomposition(n, false);

				start = System.nanoTime();
				qsim.run();
				end = System.nanoTime();

				localRoundRobinResults.add(end - start);

				System.out.println("Took " + (end - start) + "ns");
				events.printCounts();
			}

			decompositionResults.put(n, localDecompositionResults);
			roundRobinResults.put(n, localRoundRobinResults);
		}
		
		(new ObjectMapper()).writeValue(new File(outputPath), Arrays.asList(decompositionResults, roundRobinResults));
	}
	
	@SuppressWarnings("deprecation")
	QSim createQSimForDecomposition(int numberOfRunners, boolean useDecomposition) throws IOException {
		config.setParam("qsim", "numberOfThreads", String.valueOf(numberOfRunners));
		String decompositionPath = String.format("%s/decompose.%d.csv", csvPath, numberOfRunners);
		
		StaticNodeAssignmentFactory factory = new StaticNodeAssignmentFactory();
		
		if (useDecomposition) {
			new StaticNodeAssignmentCSVReader(factory).read(decompositionPath);
		}
		
		Injector injector = org.matsim.core.controler.Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new ScenarioByInstanceModule(scenario));
				bind(EventsManager.class).toInstance(events);
				install(new QSimModule());
				
				if (useDecomposition) {
					bind(QNetsimNodeAssignmentFactory.class).toInstance(factory);
				} else {
					bind(QNetsimNodeAssignmentFactory.class).to(RoundRobinNodeAssignmentFactory.class);
				}
			}
		});
		
		QSim qsim = (QSim) injector.getInstance(Mobsim.class);
		return qsim;
	}
}
