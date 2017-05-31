package playground.mas.replanning;

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import playground.mas.MASModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MASPermissibleModesCalculator implements PermissibleModesCalculator {
    final private PermissibleModesCalculator delegate;
    final private Collection<Id<Person>> ebikeUserIds;

    public MASPermissibleModesCalculator(PermissibleModesCalculator delegate, Collection<Id<Person>> ebikeUserIds) {
        this.delegate = delegate;
        this.ebikeUserIds = ebikeUserIds;
    }

    @Override
    public Collection<String> getPermissibleModes(Plan plan) {
        Collection<String> modes = delegate.getPermissibleModes(plan);

        if (ebikeUserIds.contains(plan.getPerson().getId())) {
            List<String> fixedModes = new ArrayList<>(modes);
            while (fixedModes.contains(TransportMode.bike)) fixedModes.remove(TransportMode.bike);
            if (!fixedModes.contains(MASModule.EBIKE)) fixedModes.add(MASModule.EBIKE);
            modes = fixedModes;
        }

        //System.out.println(plan.getPerson().getId().toString() + " " + modes);

        return modes;
    }
}
