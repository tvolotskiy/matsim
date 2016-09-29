package playground.sebhoerl.decomposition;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.matsim.api.core.v01.Id;

public class StaticNodeAssignmentCSVReader {
	final private StaticNodeAssignmentFactory factory;
	
	public StaticNodeAssignmentCSVReader(StaticNodeAssignmentFactory factory) {
		this.factory = factory;
	}
	
	public void read(String path) throws IOException {
		InputStream inputStream = new FileInputStream(path);
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		
		String line;
		boolean first = true;
		
		while ((line = bufferedReader.readLine()) != null) {
			if (!first) {
				String[] cols = line.split(",");
				factory.registerAssignment(Id.createNodeId(cols[0]), Integer.parseInt(cols[1]));
			} else {
				first = false;
			}
		}
		
		bufferedReader.close();
	}
}
