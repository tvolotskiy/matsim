package playground.sebhoerl.renault;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import java.io.*;
import java.util.Set;

public class LDFilterReader {
    private final Set<Id<Node>> filter;

    public LDFilterReader(Set<Id<Node>> filter) {
        this.filter = filter;
    }

    public void read(File path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        String line = null;
        while ((line = reader.readLine()) != null) {
            filter.add(Id.createNodeId(line.trim()));
        }
    }
}
