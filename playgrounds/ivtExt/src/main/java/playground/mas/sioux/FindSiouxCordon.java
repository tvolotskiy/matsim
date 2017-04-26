package playground.mas.sioux;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FindSiouxCordon {
    public static void main(String[] args) throws IOException {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(args[0]);

        Node center = network.getNodes().get(Id.createNodeId("2447909797"));
        Node border = network.getNodes().get(Id.createNodeId("80738131"));

        System.out.println(CoordUtils.calcEuclideanDistance(center.getCoord(), border.getCoord()));
    }
}
