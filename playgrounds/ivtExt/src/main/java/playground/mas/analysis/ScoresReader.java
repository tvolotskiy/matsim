package playground.mas.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;

public class ScoresReader {
    public ScoresReader(Population population, DataFrame dataFrame) {
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            Coord homeCoord = findHomeLocation(plan);

            dataFrame.scores.add(DataFrame.createScore(homeCoord.getX(), homeCoord.getY(), plan.getScore()));
        }
    }

    private Coord findHomeLocation(Plan plan) {
        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;

                if (activity.getType().equals("home")) {
                    return activity.getCoord();
                }
            }
        }

        throw new RuntimeException("No home location found.");
    }
}
