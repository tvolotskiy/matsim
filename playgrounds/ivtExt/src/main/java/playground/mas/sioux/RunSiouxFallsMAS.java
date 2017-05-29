package playground.mas.sioux;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import playground.mas.cordon.CordonCharger;
import playground.mas.MASConfigGroup;
import playground.mas.MASModule;
import playground.mas.scoring.MASScoringFunctionFactory;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import playground.sebhoerl.avtaxi.routing.AVRouteFactory;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.sebhoerl.recharging_avs.AVTravelTimeConfigGroup;
import playground.sebhoerl.recharging_avs.AVTravelTimeModule;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

public class RunSiouxFallsMAS {
    public static void main(String[] args) throws MalformedURLException, FileNotFoundException {
        String configFile = args[0];

        // 1. Configuration

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile, new AVConfigGroup(), dvrpConfigGroup, new MASConfigGroup(), new AVTravelTimeConfigGroup());
        MASModule.applyEbikes(config);

        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(AVRoute.class, new AVRouteFactory());
        ScenarioUtils.loadScenario(scenario);

        // 2. Basic controller setup

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule());
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new MASModule());
        controler.addOverridingModule(new AVTravelTimeModule());

        // 4. Run

        controler.run();
    }
}
