package contrib.baseline.runner;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;

public class RunIVTBaseline {
    static public void main(String[] args) {
        String configFile = args[0];

        // Configuration

        Config config = ConfigUtils.loadConfig(configFile, new BlackListedTimeAllocationMutatorConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Controller setup

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
        controler.addOverridingModule(new IVTBaselineScoringModule());

        // Run

        controler.run();
    }
}
