package playground.mas;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class MASAttributeUtils {
    static public boolean isEVUser(Person person) {
        if (person == null || person.getAttributes() == null) return false;
        return person.getAttributes().getAttribute(MASModule.EV) != null;
    }

    static public boolean isEbikeUser(Person person) {
        if (person == null || person.getAttributes() == null) return false;
        return person.getAttributes().getAttribute(MASModule.EBIKE) != null;
    }

    static public boolean isInnerCordon(Link link) {
        return link.getAttributes().getAttribute(MASModule.INNER_CORDON) != null;
    }

    static public boolean isOuterCordon(Link link) {
        return link.getAttributes().getAttribute(MASModule.OUTER_CORDON) != null;
    }
}
