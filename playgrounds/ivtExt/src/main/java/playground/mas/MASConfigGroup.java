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

    final static public String OUTER_CORDON_CENTER_NODE_ID = "cordonCenterNodeId";
    final static public String OUTER_CORDON_RADIUS = "cordonRadius";

    final static public String AV_POOL_CORDON_FEE = "avPoolCordonFee";
    final static public String AV_SOLO_CORDON_FEE = "avSoloCordonFee";
    final static public String EV_CORDON_FEE = "evCordonFee";
    final static public String CAR_CORDON_FEE = "carCordonFee";

    final static public String OUTER_CORDON_INTERVALS = "cordonIntervals";

    final static public String ADDITIONAL_EV_COSTS_PER_KM = "additionalEVCostsPerKm";

    final static public String INNER_CORDON_CENTER_NODE_ID = "innerCordonCenterNodeId";
    final static public String INNER_CORDON_RADIUS = "innerCordonRadius";
    final static public String INNER_CORDON_FEE_PER_KM = "innerCordonFeePerKm";
    final static public String INNER_CORDON_INTERVALS = "innerCordonIntervals";

    final static public String ANALYSIS_CENTER_NODE_ID = "analysisCenterNodeId";
    final static public String ANALYSIS_RADIUS = "analysisRadius";

    final static public String EBIKE_SPEEDUP_FACTOR = "ebikeSpeedupFactor";

    private double avSoloCordonFee = 0.0;
    private double avPoolCordonFee = 0.0;
    private double evCordonFee = 0.0;
    private double carCordonFee = 0.0;

    private Id<Node> outerCordonCenterNodeId = null;
    private double outerCordonRadius = 0.0;

    private double additionalEVCostsPerKm = 0.0;
    private String outerCordonIntervals = "";

    private Id<Node> innerCordonCenterNodeId = null;
    private double innerCordonRadius = 0.0;
    private double innerCordonFeePerKm = 0.0;
    private String innerCordonIntervals = "";

    private Id<Node> analysisCenterNodeId = null;
    private double analysisRadius = 0.0;

    private double ebikeSpeedupFactor = 1.0;

    public MASConfigGroup() {
        super(MAS);
    }

    @StringGetter(AV_POOL_CORDON_FEE)
    public double getAVPoolCordonFee() {
        return avPoolCordonFee;
    }

    @StringSetter(AV_POOL_CORDON_FEE)
    public void setAVPoolCordonFee(double avPoolCordonFee) {
        this.avPoolCordonFee = avPoolCordonFee;
    }

    @StringGetter(AV_SOLO_CORDON_FEE)
    public double getAVSoloCordonFee() {
        return avSoloCordonFee;
    }

    @StringSetter(AV_SOLO_CORDON_FEE)
    public void setAVSoloCordonFee(double avSoloCordonFee) {
        this.avSoloCordonFee = avSoloCordonFee;
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

    @StringGetter(OUTER_CORDON_CENTER_NODE_ID)
    public Id<Node> getOuterCordonCenterNodeId() {
        return outerCordonCenterNodeId;
    }

    public void setOuterCordonCenterNodeId(Id<Node> outerCordonCenterNodeId) {
        this.outerCordonCenterNodeId = outerCordonCenterNodeId;
    }

    @StringSetter(OUTER_CORDON_CENTER_NODE_ID)
    public void setOuterCordonCenterNodeId(String outerCordonCenterNodeId) {
        this.outerCordonCenterNodeId = Id.createNodeId(outerCordonCenterNodeId);
    }

    @StringGetter(OUTER_CORDON_RADIUS)
    public double getOuterCordonRadius() {
        return outerCordonRadius;
    }

    @StringSetter(OUTER_CORDON_RADIUS)
    public void setOuterCordonRadius(double radius) {
        this.outerCordonRadius = radius;
    }

    @StringGetter(ADDITIONAL_EV_COSTS_PER_KM)
    public double getAdditionalEVCostsPerKm() {
        return additionalEVCostsPerKm;
    }

    @StringSetter(ADDITIONAL_EV_COSTS_PER_KM)
    public void setAdditionalEVCostsPerKm(double additionalEVCostsPerKm) {
        this.additionalEVCostsPerKm = additionalEVCostsPerKm;
    }

    @StringGetter(OUTER_CORDON_INTERVALS)
    public String getOuterCordonIntervals() {
        return outerCordonIntervals;
    }

    @StringSetter(OUTER_CORDON_INTERVALS)
    public void setOuterCordonIntervals(String outerCordonIntervals) {
        this.outerCordonIntervals = outerCordonIntervals;
    }

    @StringGetter(INNER_CORDON_CENTER_NODE_ID)
    public Id<Node> getInnerCordonCenterNodeId() {
        return innerCordonCenterNodeId;
    }

    @StringSetter(INNER_CORDON_CENTER_NODE_ID)
    public void setInnerCordonCenterNodeId(String innerCordonCenterNodeId) {
        this.innerCordonCenterNodeId = Id.createNodeId(innerCordonCenterNodeId);
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

    @StringGetter(INNER_CORDON_INTERVALS)
    public String getInnerCordonIntervals() {
        return innerCordonIntervals;
    }

    @StringSetter(INNER_CORDON_INTERVALS)
    public void setInnerCordonIntervals(String innerCordonIntervals) {
        this.innerCordonIntervals = innerCordonIntervals;
    }

    @StringGetter(ANALYSIS_CENTER_NODE_ID)
    public Id<Node> getAnalysisCenterNodeId() {
        return analysisCenterNodeId;
    }

    public void setAnalysisCenterNodeId(Id<Node> analysisCenterNodeId) {
        this.analysisCenterNodeId = analysisCenterNodeId;
    }

    @StringSetter(ANALYSIS_CENTER_NODE_ID)
    public void setAnalysisCenterNodeId(String analysisCenterNodeId) {
        this.analysisCenterNodeId = Id.createNodeId(analysisCenterNodeId);
    }

    @StringGetter(ANALYSIS_RADIUS)
    public double getAnalysisRadius() {
        return analysisRadius;
    }

    @StringSetter(ANALYSIS_RADIUS)
    public void setAnalysisRadius(double analysisRadius) {
        this.analysisRadius = analysisRadius;
    }

    @StringGetter(EBIKE_SPEEDUP_FACTOR)
    public double getEbikeSpeedupFactor() {
        return ebikeSpeedupFactor;
    }

    @StringSetter(EBIKE_SPEEDUP_FACTOR)
    public void setEbikeSpeedupFactor(double ebikeSpeedupFactor) {
        this.ebikeSpeedupFactor = ebikeSpeedupFactor;
    }
}
