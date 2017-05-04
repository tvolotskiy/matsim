package playground.mas.scoring;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import playground.mas.cordon.CordonCharger;

import java.util.Collection;

@Singleton
public class MASScoringFunctionFactory implements ScoringFunctionFactory {
    final private ScoringFunctionFactory delegate;
    final private ScoringParametersForPerson params;
    final private CordonCharger charger;
    final private Collection<Id<Person>> evUserIds;
    final private double additionalEVCostPerKm;

    public MASScoringFunctionFactory(ScoringFunctionFactory delegate, Scenario scenario, CordonCharger charger, double additionalEVCostPerKm, Collection<Id<Person>> evUserIds) {
        this.delegate = delegate;
        this.charger = charger;
        this.evUserIds = evUserIds;
        this.additionalEVCostPerKm = additionalEVCostPerKm;

        params = new SubpopulationScoringParameters(scenario);
    }

    @Override
    public ScoringFunction createNewScoringFunction(Person person) {
        SumScoringFunction sf = (SumScoringFunction) delegate.createNewScoringFunction(person);
        sf.addScoringFunction(new MASScoringFunction(charger, person, params.getScoringParameters(person).marginalUtilityOfMoney, additionalEVCostPerKm, evUserIds.contains(person.getId())));
        return sf;
    }
}
