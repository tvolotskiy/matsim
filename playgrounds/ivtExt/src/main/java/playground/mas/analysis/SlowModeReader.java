package playground.mas.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import playground.mas.MASAttributeUtils;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.Collection;

public class SlowModeReader {
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Network network;

    public SlowModeReader(DataFrame dataFrame, BinCalculator binCalculator, Network network) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
        this.network = network;
    }

    public void read(Population population) {
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();

            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;

                    if ((leg.getMode().equals("walk") || leg.getMode().equals("bike")) && binCalculator.isCoveredValue(leg.getDepartureTime())) {
                        Link link = network.getLinks().get(leg.getRoute().getStartLinkId());

                        if (RunAnalysis.isAnalysisLink(link)) {
                            DataFrame.increment(dataFrame.passengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());
                        }

                        if (MASAttributeUtils.isInnerCordon(link)) {
                            DataFrame.increment(dataFrame.insideInnerCordonPassengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());
                        }

                        if (RunAnalysis.isInsideOuterCordon(link)) {
                            DataFrame.increment(dataFrame.insideOuterCordonPassengerDistances, leg.getMode(), binCalculator.getIndex(leg.getDepartureTime()), leg.getRoute().getDistance());
                        }
                    }
                }
            }
        }
    }
}
