package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.Collection;

public class SlowModeReader {
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Collection<Id<Link>> insideInnerCordonLinkIds;
    final private Collection<Id<Link>> insideOuterCordonLinkIds;
    final private Collection<Id<Link>> analysisLinkIds;

    public SlowModeReader(DataFrame dataFrame, BinCalculator binCalculator, Collection<Id<Link>> insideInnerCordonLinkIds, Collection<Id<Link>> insideOuterCordonLinkIds, Collection<Id<Link>> analysisLinkIds) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
        this.insideInnerCordonLinkIds = insideInnerCordonLinkIds;
        this.insideOuterCordonLinkIds = insideOuterCordonLinkIds;
        this.analysisLinkIds = analysisLinkIds;
    }

    public void read(Population population) {
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();

            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;

                    if ((leg.getMode().equals("walk") || leg.getMode().equals("bike")) && binCalculator.isCoveredValue(leg.getDepartureTime())) {
                        if (analysisLinkIds.contains(leg.getRoute().getStartLinkId())) {
                            DataFrame.increment(dataFrame.passengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());
                        }

                        if (insideInnerCordonLinkIds.contains(leg.getRoute().getStartLinkId())) {
                            DataFrame.increment(dataFrame.insideInnerCordonPassengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());
                        }

                        if (insideOuterCordonLinkIds.contains(leg.getRoute().getStartLinkId())) {
                            DataFrame.increment(dataFrame.insideOuterCordonPassengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());
                        }
                    }
                }
            }
        }
    }
}
