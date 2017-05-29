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

    final static public String AV_CORDON_FEE = "avCordonFee";
    final static public String EV_CORDON_FEE = "evCordonFee";
    final static public String CAR_CORDON_FEE = "carCordonFee";

    final static public String CORDON_INTERVALS = "cordonIntervals";
    final static public String CHARGED_OPERATORS = "chargedOperators";

    final static public String ADDITIONAL_EV_COSTS_PER_KM = "additionalEVCostsPerKm";

    final static public String INNER_CORDON_CENTER_NODE_ID = "innerCordonCenterNodeId";
    final static public String INNER_CORDON_RADIUS = "innerCordonRadius";
    final static public String INNER_CORDON_FEE_PER_KM = "innerCordonFeePerKm";

    private double avCordonFee = 0.0;
    private double evCordonFee = 0.0;
    private double carCordonFee = 0.0;

    private Set<Id<AVOperator>> chargedOperators = new HashSet<>();

    private Id<Node> cordonCenterNodeId = null;
    private double cordonRadius = 0.0;

    private double additionalEVCostsPerKm = 0.0;
    private String cordonIntervals = "";

    private Id<Node> innerCordonCenterNodeId = null;
    private double innerCordonRadius = 0.0;
    private double innerCordonFeePerKm = 0.0;

    public MASConfigGroup() {
        super(MAS);
    }

    @StringGetter(AV_CORDON_FEE)
    public double getAVCordonFee() {
        return avCordonFee;
    }

    @StringSetter(AV_CORDON_FEE)
    public void setAVCordonFee(double avCordonFee) {
        this.avCordonFee = avCordonFee;
    }

    @StringGetter(EV_CORDON_FEE)
    public double getEVCordonFee() {
        return evCordonFee;
    }

    @StringSetter(EV_CORDON_FEE)
    public void setEVCordonFee(double evCordonFee) {
        this.evCordonFee = evCordonFee;
    }

    @StringGetter(CAR_CORDON_FEE)
    public double getCarCordonFee() {
        return carCordonFee;
    }

    @StringSetter(CAR_CORDON_FEE)
    public void setCarCordonFee(double carCordonFee) {
        this.carCordonFee = carCordonFee;
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

    @StringGetter(ADDITIONAL_EV_COSTS_PER_KM)
    public double getAdditionalEVCostsPerKm() {
        return additionalEVCostsPerKm;
    }

    @StringSetter(ADDITIONAL_EV_COSTS_PER_KM)
    public void setAdditionalEVCostsPerKm(double additionalEVCostsPerKm) {
        this.additionalEVCostsPerKm = additionalEVCostsPerKm;
    }

    @StringGetter(CORDON_INTERVALS)
    public String getCordonIntervals() {
        return cordonIntervals;
    }

    @StringSetter(CORDON_INTERVALS)
    public void setCordonIntervals(String cordonIntervals) {
        this.cordonIntervals = cordonIntervals;
    }

    @StringGetter(INNER_CORDON_CENTER_NODE_ID)
    public Id<Node> getInnerCordonCenterNodeId() {
        return innerCordonCenterNodeId;
    }

    @StringSetter(INNER_CORDON_CENTER_NODE_ID)
    public void setInnerCordonCenterNodeId(Id<Node> innerCordonCenterNodeId) {
        this.innerCordonCenterNodeId = innerCordonCenterNodeId;
    }

    @StringGetter(INNER_CORDON_RADIUS)
    public double getInnerCordonRadius() {
        return innerCordonRadius;
    }

    @StringSetter(INNER_CORDON_RADIUS)
    public void setInnerCordonRadius(double innerCordonRadius) {
        this.innerCordonRadius = innerCordonRadius;
    }

    @StringGetter(INNER_CORDON_FEE_PER_KM)
    public double getInnerCordonFeePerKm() {
        return innerCordonFeePerKm;
    }

    @StringSetter(INNER_CORDON_FEE_PER_KM)
    public void setInnerCordonFeePerKm(double innerCordonFeePerKm) {
        this.innerCordonFeePerKm = innerCordonFeePerKm;
    }
}
