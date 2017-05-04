package playground.mas.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;

public class ScoresReader {
    final private DataFrame dataFrame;

    public ScoresReader(DataFrame dataFrame) {
        this.dataFrame = dataFrame;
    }

    public void read(Population population) {
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            Coord homeCoord = findHomeLocation(plan);

            if (homeCoord != null) {
                dataFrame.scores.add(DataFrame.createScore(homeCoord.getX(), homeCoord.getY(), plan.getScore()));
            }
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

        return null;
    }
}
