package playground.mas.sioux;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FindSiouxCordon {
    public static void main(String[] args) throws IOException {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(args[0]);

        FileInputStream stream = new FileInputStream(args[1]);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        final Set<Node> insideNodes = new HashSet<>();
        final Set<Link> insideLinks = new HashSet<>();

        reader.lines().forEach((String nodeId) -> insideNodes.add(network.getNodes().get(Id.createNodeId(nodeId))) );
        insideNodes.forEach((Node node) -> insideLinks.addAll(node.getOutLinks().values()));
        insideNodes.forEach((Node node) -> insideLinks.addAll(node.getInLinks().values()));

        Set<Link> cordonLinks = insideLinks.stream().filter((l) ->
                l.getAllowedModes().contains("car") && !insideNodes.contains(l.getFromNode())
        ).collect(Collectors.toSet());

        FileOutputStream outputStream = new FileOutputStream(args[2]);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        for (Link link : cordonLinks) {
            writer.write(link.getId().toString() + "\n");
        }

        writer.flush();
        writer.close();
    }
}
