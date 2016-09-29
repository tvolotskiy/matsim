package playground.sebhoerl.decomposition;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.assignment.QNetsimNodeAssignmentFactory;
import org.matsim.core.scenario.ScenarioUtils;

public class RunDecompositionScenario {
	public static void main(String[] args) throws IOException {
		String configPath = args[0]; // /home/sebastian/sioux-2016/config.xml
		String csvPath = args[1]; // /home/sebastian/decomposition/sioux.csv
		
		Config config = ConfigUtils.loadConfig(configPath);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		StaticNodeAssignmentFactory factory = new StaticNodeAssignmentFactory();
		new StaticNodeAssignmentCSVReader(factory).read(csvPath);
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(QNetsimNodeAssignmentFactory.class).toInstance(factory);
			}
		});
		
		controler.run();
	}
}
