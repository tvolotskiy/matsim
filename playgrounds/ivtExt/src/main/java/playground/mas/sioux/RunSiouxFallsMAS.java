package playground.mas.sioux;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import contrib.baseline.IVTBaselineScoringFunctionFactory;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.PtConstants;
import org.matsim.vehicles.Vehicle;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;
import playground.mas.CordonCharger;
import playground.mas.MASConfigGroup;
import playground.mas.MASModule;
import playground.mas.scoring.MASScoringFunctionFactory;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.zurich_av.RunZurichWithAV;
import playground.zurich_av.ZurichGenerator;
import playground.zurich_av.replanning.ZurichPlanStrategyProvider;

import javax.inject.Named;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RunSiouxFallsMAS {
    public static void main(String[] args) throws MalformedURLException, FileNotFoundException {
        String configFile = args[0];

        // 1. Configuration

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile, new AVConfigGroup(), dvrpConfigGroup, new MASConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // 2. Basic controller setup

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule());
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new MASModule());

        // 3. Scoring

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {}

            @Provides @Singleton
            ScoringFunctionFactory provideScoringFunctionFactory(AVScoringFunctionFactory delegate, Scenario scenario, AVConfig config, CordonCharger charger) {
                return new MASScoringFunctionFactory(delegate, scenario, charger);
            }
        });

        // 4. Run

        controler.run();
    }
}
