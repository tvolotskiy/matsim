package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.Collection;

public class SlowModeReader {
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Collection<Id<Link>> insideCordonLinkIds;

    public SlowModeReader(DataFrame dataFrame, BinCalculator binCalculator, Collection<Id<Link>> insideCordonLinkIds) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
        this.insideCordonLinkIds = insideCordonLinkIds;
    }

    public void read(Population population) {
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();

            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;

                    if ((leg.getMode().equals("walk") || leg.getMode().equals("bike")) && binCalculator.isCoveredValue(leg.getDepartureTime())) {
                        DataFrame.increment(dataFrame.passengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());

                        if (insideCordonLinkIds.contains(leg.getRoute().getStartLinkId())) {
                            DataFrame.increment(dataFrame.insidePassengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());
                        }
                    }
                }
            }
        }
    }
}
