package playground.sebhoerl.recharging_avs;

import org.matsim.core.config.ReflectiveConfigGroup;

public class AVTravelTimeConfigGroup extends ReflectiveConfigGroup {
    final static public String AV_TRAVEL_TIME = "av_travel_time";
    final static public String TRAVEL_TIME_TYPE = "travelTimeType";

    public enum AVTravelTimeType {
        ITERATIVE, ONLINE
    }

    private AVTravelTimeType travelTimeType = AVTravelTimeType.ITERATIVE;

    public AVTravelTimeConfigGroup() {
        super(AV_TRAVEL_TIME);
    }

    @StringGetter(TRAVEL_TIME_TYPE)
    public AVTravelTimeType getTravelTimeType() {
        return travelTimeType;
    }

    @StringSetter(TRAVEL_TIME_TYPE)
    public void setTravelTimeType(AVTravelTimeType travelTimeType) {
        this.travelTimeType = travelTimeType;
    }
}
