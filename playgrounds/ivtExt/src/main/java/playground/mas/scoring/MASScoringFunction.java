package playground.mas.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction;
import playground.mas.CordonCharger;

public class MASScoringFunction implements SumScoringFunction.BasicScoring {
    final private CordonCharger charger;
    final private double marginalUtilityOfMoney;
    final private Id<Person> personId;

    public MASScoringFunction(CordonCharger charger, Person person, double marginalUtilityOfMoney) {
        this.charger = charger;
        this.personId = person.getId();
        this.marginalUtilityOfMoney = marginalUtilityOfMoney;
    }

    @Override
    public double getScore() {
        return -marginalUtilityOfMoney * charger.getCharge(personId);
    }

    @Override
    public void finish() {}
}
