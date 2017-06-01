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

        Collection<Id<Link>> insideInnerCordonLinkIds = MASCordonUtils.findInsideCordonLinks(masConfigGroup.getInnerCordonCenterNodeId(), masConfigGroup.getInnerCordonRadius(), network)
                .stream().map(l -> l.getId()).collect(Collectors.toList());
        Collection<Id<Link>> insideOuterCordonLinkIds = MASCordonUtils.findInsideCordonLinks(masConfigGroup.getOuterCordonCenterNodeId(), masConfigGroup.getOuterCordonRadius(), network)
                .stream().map(l -> l.getId()).collect(Collectors.toList());

        Collection<Id<Link>> outerCordonLinkIds = MASCordonUtils.findChargeableCordonLinks(masConfigGroup.getOuterCordonCenterNodeId(), masConfigGroup.getOuterCordonRadius(), network)
                .stream().map(l -> l.getId()).collect(Collectors.toList());

        Collection<Id<Link>> analysisLinkIds = MASCordonUtils.findInsideCordonLinks(masConfigGroup.getAnalysisCenterNodeId(), masConfigGroup.getAnalysisRadius(), network)
                .stream().map(l -> l.getId()).collect(Collectors.toList());

        MASModule masModule = new MASModule();

        Collection<Id<Person>> evPersonIds = scenario.getPopulation().getPersons().values().stream()
                .filter(p -> MASAttributeUtils.isEVUser(p))
                .map(p -> p.getId())
                .collect(Collectors.toSet());

        CordonState innerCordonState = masModule.provideInnerCordonState(masConfigGroup);
        CordonState outerCordonState = masModule.provideOuterCordonState(masConfigGroup);

        CordonPricing cordonPricing = new CordonPricing(masConfigGroup, scenario.getNetwork(), innerCordonState, outerCordonState);
        ChargeTypeFinder chargeTypeFinder = new ChargeTypeFinder(scenario.getPopulation());

        BinCalculator binCalculator = BinCalculator.createByInterval(0.0, 24.0 * 3600.0, 300.0);
        DataFrame dataFrame = new DataFrame(binCalculator);

        EventsManager eventsManager = EventsUtils.createEventsManager(config);
        eventsManager.addHandler(new CountsHandler(dataFrame, binCalculator, insideInnerCordonLinkIds, insideOuterCordonLinkIds, analysisLinkIds, evPersonIds));
        eventsManager.addHandler(new CordonHandler(dataFrame, binCalculator, outerCordonLinkIds, insideInnerCordonLinkIds, cordonPricing, chargeTypeFinder, network));
        eventsManager.addHandler(new DistancesHandler(dataFrame, binCalculator, scenario.getNetwork(), evPersonIds, insideInnerCordonLinkIds, insideOuterCordonLinkIds, analysisLinkIds));
        eventsManager.addHandler(new AVHandler(dataFrame, binCalculator));

        new MatsimEventsReader(eventsManager).readFile(eventsPath);
        new ScoresReader(dataFrame).read(scenario.getPopulation());
        new SlowModeReader(dataFrame, binCalculator, insideInnerCordonLinkIds, insideOuterCordonLinkIds, analysisLinkIds).read(scenario.getPopulation());

        (new ObjectMapper()).writeValue(new File(outputPath), dataFrame);
    }
}
