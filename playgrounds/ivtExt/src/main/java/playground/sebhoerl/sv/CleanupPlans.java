package playground.sebhoerl.sv;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.Set;

public class CleanupPlans {
    static public void main(String[] args) {
        Scenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);

        Set<Id<Person>> delete = new HashSet<>();

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;
                        Coord coord = activity.getCoord();

                        if (Double.isNaN(coord.getY()) || Double.isNaN(coord.getX())) {
                            delete.add(person.getId());
                        }
                    }
                }
            }
        }

        for (Id<Person> personId : delete) {
            scenario.getPopulation().removePerson(personId);
        }

        new PopulationWriter(scenario.getPopulation()).write(args[1]);
    }
}
