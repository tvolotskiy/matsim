package playground.mas.replanning;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import playground.mas.MASModule;
import playground.mas.MASAttributeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MASPermissibleModesCalculator implements PermissibleModesCalculator {
    final private PermissibleModesCalculator delegate;

    public MASPermissibleModesCalculator(PermissibleModesCalculator delegate) {
        this.delegate = delegate;
    }

    @Override
    public Collection<String> getPermissibleModes(Plan plan) {
        Collection<String> modes = delegate.getPermissibleModes(plan);

        if (MASAttributeUtils.isEbikeUser(plan.getPerson())) {
            List<String> fixedModes = new ArrayList<>(modes);
            while (fixedModes.contains(TransportMode.bike)) fixedModes.remove(TransportMode.bike);
            if (!fixedModes.contains(MASModule.EBIKE)) fixedModes.add(MASModule.EBIKE);
            modes = fixedModes;
        }

        return modes;
    }
}
