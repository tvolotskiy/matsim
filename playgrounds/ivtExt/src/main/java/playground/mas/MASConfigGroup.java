package playground.mas;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ReflectiveConfigGroup;
import playground.sebhoerl.avtaxi.data.AVOperator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MASConfigGroup extends ReflectiveConfigGroup {
    final static public String MAS = "mas";

    final static public String CORDON_CENTER_NODE_ID = "cordonCenterNodeId";
    final static public String CORDON_RADIUS = "cordonRadius";

    final static public String CORDON_FEE = "cordonFee";
    final static public String CHARGED_OPERATORS = "chargedOperators";

    private double cordonFee = 0.0;
    private Set<Id<AVOperator>> chargedOperators = new HashSet<>();

    private Id<Node> cordonCenterNodeId = null;
    private double cordonRadius = 0.0;

    public MASConfigGroup() {
        super(MAS);
    }

    @StringGetter(CORDON_FEE)
    public double getCordonFee() {
        return cordonFee;
    }

    @StringSetter(CORDON_FEE)
    public void setCordonFee(double cordonFee) {
        this.cordonFee = cordonFee;
    }

    @StringGetter(CHARGED_OPERATORS)
    public String getChargedOperators() {
        return String.join(",", chargedOperators.stream().map(i -> i.toString()).collect(Collectors.toList()));
    }

    @StringSetter(CHARGED_OPERATORS)
    public void setChargedOperators(String chargedOperators) {
        this.chargedOperators = Arrays.asList(chargedOperators.split(",")).stream().map(i -> Id.create(i, AVOperator.class)).collect(Collectors.toSet());
    }

    public Collection<Id<AVOperator>> getChargedOperatorIds() {
        return chargedOperators;
    }

    @StringGetter(CORDON_CENTER_NODE_ID)
    public Id<Node> getCordonCenterNodeId() {
        return cordonCenterNodeId;
    }

    public void setCordonCenterNodeId(Id<Node> cordonCenterNodeId) {
        this.cordonCenterNodeId = cordonCenterNodeId;
    }

    @StringSetter(CORDON_CENTER_NODE_ID)
    public void setCordonCenterNodeId(String cordonCenterNodeId) {
        this.cordonCenterNodeId = Id.createNodeId(cordonCenterNodeId);
    }

    @StringGetter(CORDON_RADIUS)
    public double getCordonRadius() {
        return cordonRadius;
    }

    @StringSetter(CORDON_RADIUS)
    public void setCordonRadius(double radius) {
        this.cordonRadius = radius;
    }
}
