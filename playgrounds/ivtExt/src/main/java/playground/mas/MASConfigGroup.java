package playground.mas;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ReflectiveConfigGroup;
import playground.sebhoerl.avtaxi.data.AVOperator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MASConfigGroup extends ReflectiveConfigGroup {
    final static public String MAS = "mas";

    final static public String CORDON_PRICE = "cordonPrice";
    final static public String CHARGED_OPERATORS = "chargedOperators";
    final static public String INPUT_CORDON = "inputCordonFile";

    private double cordonPrice = 0.0;
    private Set<Id<AVOperator>> chargedOperators = new HashSet<>();
    private String cordonPath;

    public MASConfigGroup() {
        super(MAS);
    }

    @StringGetter(CORDON_PRICE)
    public double getCordonPrice() {
        return cordonPrice;
    }

    @StringSetter(CORDON_PRICE)
    public void setCordonPrice(double cordonPrice) {
        this.cordonPrice = cordonPrice;
    }

    @StringGetter(CHARGED_OPERATORS)
    public String _getChargedOperators() {
        return String.join(",", chargedOperators.stream().map(i -> i.toString()).collect(Collectors.toList()));
    }

    public Collection<Id<AVOperator>> getChargedOperators() {
        return chargedOperators;
    }

    @StringSetter(CHARGED_OPERATORS)
    public void setChargedOperators(String chargedOperators) {
        this.chargedOperators = Arrays.asList(chargedOperators.split(",")).stream().map(i -> Id.create(i, AVOperator.class)).collect(Collectors.toSet());
    }

    @StringGetter(INPUT_CORDON)
    public String getCordonPath() {
        return cordonPath;
    }

    @StringSetter(INPUT_CORDON)
    public void setCordonPath(String cordonPath) {
        this.cordonPath = cordonPath;
    }
}
