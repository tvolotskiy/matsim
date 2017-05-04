package playground.mas.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction;
import playground.mas.cordon.CordonCharger;

public class MASScoringFunction implements SumScoringFunction.BasicScoring, SumScoringFunction.LegScoring {
    final private CordonCharger charger;
    final private double marginalUtilityOfMoney;
    final private Id<Person> personId;
    final private boolean isEVUser;
    final private double additionalEVCostPerKm;

    private double score = 0.0;

    public MASScoringFunction(CordonCharger charger, Person person, double marginalUtilityOfMoney, double additionalEVCostPerKm, boolean isEVUser) {
        this.charger = charger;
        this.personId = person.getId();
        this.marginalUtilityOfMoney = marginalUtilityOfMoney;
        this.additionalEVCostPerKm = additionalEVCostPerKm;
        this.isEVUser = isEVUser;
    }

    @Override
    public double getScore() {
        return score - marginalUtilityOfMoney * charger.getCharge(personId);
    }

    @Override
    public void finish() {}

    @Override
    public void handleLeg(Leg leg) {
        if (leg.getMode().equals("car") && isEVUser) {
            score += additionalEVCostPerKm * marginalUtilityOfMoney * leg.getRoute().getDistance() / 1000.0;
        }
    }
}
