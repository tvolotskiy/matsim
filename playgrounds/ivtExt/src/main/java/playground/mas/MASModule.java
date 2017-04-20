package playground.mas;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import playground.mas.scoring.MASScoringFunctionFactory;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.zurich_av.RunZurichWithAV;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MASModule extends AbstractModule {
    final public static String CORDON_LINKS = "cordon_links";
    final private Logger log = Logger.getLogger(MASModule.class);

    @Override
    public void install() {
        addEventHandlerBinding().to(CordonCharger.class);
        bind(ScoringFunctionFactory.class).to(MASScoringFunctionFactory.class);
    }

    @Provides @Singleton
    private CordonCharger provideCordonCharger(@Named(CORDON_LINKS) Collection<Link> cordonLinks, MASConfigGroup config, @Named("ev_user_ids") Collection<Id<Person>> evUserIds) {
        return new CordonCharger(cordonLinks, config.getCordonPrice(), config.getChargedOperators(), evUserIds);
    }

    @Provides @Singleton @Named(CORDON_LINKS)
    public Collection<Link> provideCordonLinks(Config config, MASConfigGroup masConfig, Network network) {
        try {
            FileInputStream stream = new FileInputStream(ConfigGroup.getInputFileURL(config.getContext(), masConfig.getCordonPath()).getPath());
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            final Set<Node> insideNodes = new HashSet<>();
            final Set<Link> insideLinks = new HashSet<>();

            reader.lines().forEach((String nodeId) -> insideNodes.add(network.getNodes().get(Id.createNodeId(nodeId))) );
            insideNodes.forEach((Node node) -> insideLinks.addAll(node.getOutLinks().values()));
            insideNodes.forEach((Node node) -> insideLinks.addAll(node.getInLinks().values()));

            Set<Link> cordonLinks = insideLinks.stream().filter((l) ->
                    l.getAllowedModes().contains("car") && !insideNodes.contains(l.getFromNode())
            ).collect(Collectors.toSet());

            log.info("Loaded " + cordonLinks.size() + " cordon links (from " + insideNodes.size() + " included nodes).");
            return cordonLinks;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cordon input file not found.");
        }
    }

    @Provides @Singleton
    public MASScoringFunctionFactory provideScoringFunctionFactory(AVScoringFunctionFactory delegate, Scenario scenario, AVConfig config, CordonCharger charger) {
        return new MASScoringFunctionFactory(delegate, scenario, charger);
    }

    @Provides @Singleton @Named("ev_user_ids")
    public Collection<Id<Person>> provideEVUserIds(Population population) {
        return population.getPersons().keySet().stream().filter(new Predicate<Id<Person>>() {
            @Override
            public boolean test(Id<Person> personId) {
                Boolean flag = (Boolean) population.getPersonAttributes().getAttribute(personId.toString(), "ev");
                return flag != null && flag;
            }
        }).collect(Collectors.toSet());
    }
}
