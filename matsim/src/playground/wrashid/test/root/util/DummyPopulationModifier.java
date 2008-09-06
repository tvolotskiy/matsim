package playground.wrashid.test.root.util;

import org.matsim.population.Population;

public class DummyPopulationModifier implements PopulationModifier {
	Population population=null;
	
	// does not modify population at all
	// needed to pass population to tests
	public Population modifyPopulation(Population population) {
		this.population=population;
		return population;
	}

	public Population getPopulation(){
		return population;
	} 

}
