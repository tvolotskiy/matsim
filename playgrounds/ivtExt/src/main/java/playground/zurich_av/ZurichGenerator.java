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
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.generator.AVGenerator;
import playground.zurich_av.replanning.ZurichAVLinkChecker;

import java.util.*;

public class ZurichGenerator implements AVGenerator {
    final private AVOperator operator;
    final private Network network;
    final private long numberOfVehicles;

    private List<Id<Link>> linkCache;
    private long generatedVehicleCount = 0;

    public ZurichGenerator(Network network, ZurichAVLinkChecker linkChecker, long numberOfVehicles, AVOperator operator) {
        this.network = network;
        this.operator = operator;
        this.numberOfVehicles = numberOfVehicles;

        this.linkCache = new LinkedList<>();

        for (Link link : network.getLinks().values()) {
            if (linkChecker.isAcceptable(link)) {
                this.linkCache.add(link.getId());
            }
        }
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
        @Inject
        private ZurichAVLinkChecker linkChecker;

        @Inject
        Network network;

        @Inject
        private Map<Id<AVOperator>, AVOperator> operators;

        @Override
        public AVGenerator createGenerator(AVGeneratorConfig generatorConfig) {
            long numberOfVehicles = generatorConfig.getNumberOfVehicles();
            return new ZurichGenerator(network, linkChecker, numberOfVehicles, operators.get(generatorConfig.getParent().getId()));
        }
    }
}
