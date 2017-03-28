package playground.sebhoerl.recharging_avs;

import org.matsim.core.config.ReflectiveConfigGroup;

public class RechargingConfig extends ReflectiveConfigGroup {
    final static public String RECHARGING = "recharging";
    final static public String TRACK_CONSUMPTION = "trackConsumption";

    private boolean trackConsumption = false;

    public RechargingConfig() {
        super(RECHARGING);
    }

    @StringGetter(TRACK_CONSUMPTION)
    public boolean getTrackConsumption() {
        return trackConsumption;
    }

    @StringSetter(TRACK_CONSUMPTION)
    public void setTrackConsumption(boolean trackConsumption) {
        this.trackConsumption = trackConsumption;
    }
}
