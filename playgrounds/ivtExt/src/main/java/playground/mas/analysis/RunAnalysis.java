package playground.mas.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.mas.MASAttributeUtils;
import playground.mas.MASConfigGroup;
import playground.mas.MASModule;
import playground.mas.cordon.*;
import playground.mas.zurich.ZurichMASConfigGroup;
import playground.sebhoerl.av_paper.BinCalculator;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

public class RunAnalysis {
    static public void main(String[] args) throws IOException {
        String configPath = args[0];
        String eventsPath = args[1];
        String populationPath = args[2];
        String outputPath = args[3];

        ZurichMASConfigGroup zurichMASConfigGroup = new ZurichMASConfigGroup();
        MASConfigGroup masConfigGroup = new MASConfigGroup();

        Config config = ConfigUtils.loadConfig(configPath, masConfigGroup, zurichMASConfigGroup);
        config.plans().setInputFile(populationPath);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        for (Link link : MASCordonUtils.findInsideCordonLinks(masConfigGroup.getAnalysisCenterNodeId(), masConfigGroup.getAnalysisRadius(), network)) {
            link.getAttributes().putAttribute("analyse", true);
        }

        for (Link link : MASCordonUtils.findInsideCordonLinks(masConfigGroup.getOuterCordonCenterNodeId(), masConfigGroup.getOuterCordonRadius(), network)) {
            link.getAttributes().putAttribute("inside_outer", true);
        }

        MASModule masModule = new MASModule();

        CordonState innerCordonState = masModule.provideInnerCordonState(masConfigGroup);
        CordonState outerCordonState = masModule.provideOuterCordonState(masConfigGroup);

        CordonPricing cordonPricing = new CordonPricing(masConfigGroup, scenario.getNetwork(), innerCordonState, outerCordonState);
        ChargeTypeFinder chargeTypeFinder = new ChargeTypeFinder(scenario.getPopulation());

        BinCalculator binCalculator = BinCalculator.createByInterval(0.0, 24.0 * 3600.0, 300.0);
        DataFrame dataFrame = new DataFrame(binCalculator);

        EventsManager eventsManager = EventsUtils.createEventsManager(config);
        eventsManager.addHandler(new CountsHandler(dataFrame, binCalculator, scenario.getNetwork(), scenario.getPopulation()));
        eventsManager.addHandler(new CordonHandler(dataFrame, binCalculator, cordonPricing, chargeTypeFinder, network));
        eventsManager.addHandler(new DistancesHandler(dataFrame, binCalculator, scenario.getNetwork(), scenario.getPopulation()));
        eventsManager.addHandler(new AVHandler(dataFrame, binCalculator));

        new MatsimEventsReader(eventsManager).readFile(eventsPath);
        new ScoresReader(dataFrame).read(scenario.getPopulation());
        new SlowModeReader(dataFrame, binCalculator, scenario.getNetwork()).read(scenario.getPopulation());

        (new ObjectMapper()).writeValue(new File(outputPath), dataFrame);
    }

    static boolean isAnalysisLink(Link link) {
        if (link == null) return false;
        return link.getAttributes().getAttribute("analyse") != null;
    }

    static boolean isInsideOuterCordon(Link link) {
        if (link == null) return false;
        return link.getAttributes().getAttribute("inside_outer") != null;
    }
}
