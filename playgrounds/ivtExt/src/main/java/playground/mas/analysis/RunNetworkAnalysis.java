package playground.mas.analysis;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import playground.mas.MASConfigGroup;
import playground.mas.cordon.MASCordonUtils;

import java.io.*;
import java.util.*;

public class RunNetworkAnalysis {
    public static void main(String[] args) throws FileNotFoundException {
        MASConfigGroup masConfigGroup = new MASConfigGroup();
        Config config = ConfigUtils.loadConfig(args[0], masConfigGroup);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(ConfigGroup.getInputFileURL(config.getContext(), config.network().getInputFile()).getPath());

        Collection<Link> cordonLinks = MASCordonUtils.findChargeableCordonLinks(masConfigGroup.getOuterCordonCenterNodeId(), masConfigGroup.getOuterCordonRadius(), network);
        Collection<Link> insideLinks = MASCordonUtils.findInsideCordonLinks(masConfigGroup.getOuterCordonCenterNodeId(), masConfigGroup.getOuterCordonRadius(), network);

        for (Link link : network.getLinks().values()) {
            link.getAttributes().putAttribute("is_cordon", cordonLinks.contains(link));
            link.getAttributes().putAttribute("is_inside", insideLinks.contains(link));
        }

        new NetworkWriter(network).write("attr_network.xml");
    }
}
