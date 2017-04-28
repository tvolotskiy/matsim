package playground.mas.sioux;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import playground.mas.MASConfigGroup;
import playground.mas.cordon.MASCordonUtils;

import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

public class GenerateSiouxTestPopulation {
    static public void main(String[] args) {
        MASConfigGroup masConfig = new MASConfigGroup();
        Config config = ConfigUtils.loadConfig(args[0], masConfig);
        config.plans().setInputFile("population.xml.gz");

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(ConfigGroup.getInputFileURL(config.getContext(), config.network().getInputFile()).getPath());

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PopulationFactory factory = population.getFactory();

        Collection<Link> insideLinks = MASCordonUtils.findInsideCordonLinks(masConfig.getCordonCenterNodeId(), masConfig.getCordonRadius(), network);
        Collection<Link> outsideLinks = network.getLinks().values().stream().filter(l -> !insideLinks.contains(l)).collect(Collectors.toList());

        for (int i = 0; i < 100; i++) {
            Person person = factory.createPerson(Id.createPersonId("person" + i));
            population.addPerson(person);

            Plan plan = factory.createPlan();
            person.addPlan(plan);
            person.setSelectedPlan(plan);

            Link homeLink = outsideLinks.stream().skip(new Random().nextInt(outsideLinks.size())).findFirst().get();
            Link workLink = insideLinks.stream().skip(new Random().nextInt(insideLinks.size())).findFirst().get();

            Activity startActivity = factory.createActivityFromLinkId("home", homeLink.getId());
            startActivity.setCoord(homeLink.getCoord());
            startActivity.setEndTime(7 * 3600.0);

            Leg startLeg = factory.createLeg("car");

            Activity centerActivity = factory.createActivityFromLinkId("work", workLink.getId());
            centerActivity.setCoord(workLink.getCoord());
            centerActivity.setStartTime(8 * 3600.0);
            centerActivity.setEndTime(9 * 3600.0);

            Leg endLeg = factory.createLeg("car");

            Activity endActivity = factory.createActivityFromLinkId("home", homeLink.getId());
            endActivity.setCoord(homeLink.getCoord());
            endActivity.setStartTime(10 * 3600.0);

            plan.addActivity(startActivity);
            plan.addLeg(startLeg);
            plan.addActivity(centerActivity);
            plan.addLeg(endLeg);
            plan.addActivity(endActivity);

        }

        new PopulationWriter(population).write(args[1]);
    }
}
