package playground.zurich_av;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.gbl.MatsimRandom;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.generator.AVGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ZurichGenerator implements AVGenerator {
    final private AVOperator operator;
    final private Collection<Id<Link>> permissibleLinkIds;
    final private Network network;
    final private long numberOfVehicles;

    private ArrayList<Id<Link>> linkCache;
    private long generatedVehicleCount = 0;

    public ZurichGenerator(Network network, Collection<Id<Link>> permissibleLinkIds, long numberOfVehicles, AVOperator operator) {
        this.network = network;
        this.operator = operator;
        this.permissibleLinkIds = permissibleLinkIds;
        this.numberOfVehicles = numberOfVehicles;
        this.linkCache = new ArrayList<>(permissibleLinkIds);
    }

    @Override
    public boolean hasNext() {
        if (generatedVehicleCount >= numberOfVehicles) {
            linkCache = null;
            return false;
        }

        return true;
    }

    @Override
    public AVVehicle next() {
        generatedVehicleCount++;

        return new AVVehicle(
                Id.create("av_" + operator.getId().toString() + "_" + generatedVehicleCount, Vehicle.class),
                network.getLinks().get(linkCache.get(MatsimRandom.getRandom().nextInt(linkCache.size()))),
                4.0,
                0.0,
                108000.0,
                operator
                );
    }

    static public class ZurichGeneratorFactory implements AVGeneratorFactory {
        @Inject @Named("zurich")
        private Collection<Id<Link>> permissibleLinkIds;

        @Inject
        Network network;

        @Inject
        private Map<Id<AVOperator>, AVOperator> operators;

        @Override
        public AVGenerator createGenerator(AVGeneratorConfig generatorConfig) {
            long numberOfVehicles = generatorConfig.getNumberOfVehicles();
            return new ZurichGenerator(network, permissibleLinkIds, numberOfVehicles, operators.get(generatorConfig.getParent().getId()));
        }
    }
}
