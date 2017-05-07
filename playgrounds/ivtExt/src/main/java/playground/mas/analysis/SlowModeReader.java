package playground.mas.analysis;

import org.matsim.api.core.v01.population.*;
import playground.sebhoerl.av_paper.BinCalculator;

public class SlowModeReader {
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    public SlowModeReader(DataFrame dataFrame, BinCalculator binCalculator) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
    }

    public void read(Population population) {
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();

            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;

                    if ((leg.getMode().equals("walk") || leg.getMode().equals("bike")) && binCalculator.isCoveredValue(leg.getDepartureTime())) {
                        DataFrame.increment(dataFrame.passengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());
                    }
                }
            }
        }
    }
}
