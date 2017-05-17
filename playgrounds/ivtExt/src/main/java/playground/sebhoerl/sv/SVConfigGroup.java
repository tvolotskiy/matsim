package playground.sebhoerl.sv;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ReflectiveConfigGroup;

public class SVConfigGroup extends ReflectiveConfigGroup {
    final static public String SOMMERVILLE = "sv";

    final static public String AV_AREA_CENTER_NODE_ID = "avAreaCenterNodeId";
    final static public String AV_AREA_RADIUS = "avAreaRadius";

    public SVConfigGroup() {
        super(SOMMERVILLE);
    }

    private Id<Node> avAreaCenterNodeId = null;
    private double avAreaRadius = 0.0;

    @StringGetter(AV_AREA_CENTER_NODE_ID)
    public Id<Node> getAVAreaCenterNodeId() {
        return avAreaCenterNodeId;
    }

    public void setAVAreaCenterNodeId(Id<Node> cordonCenterNodeId) {
        this.avAreaCenterNodeId = avAreaCenterNodeId;
    }

    @StringSetter(AV_AREA_CENTER_NODE_ID)
    public void setAVAreaCenterNodeId(String avAreaCenterNodeId) {
        this.avAreaCenterNodeId = Id.createNodeId(avAreaCenterNodeId);
    }

    @StringGetter(AV_AREA_RADIUS)
    public double getAVAreaRadius() {
        return avAreaRadius;
    }

    @StringSetter(AV_AREA_RADIUS)
    public void setAVAreaRadius(double radius) {
        this.avAreaRadius = radius;
    }
}
