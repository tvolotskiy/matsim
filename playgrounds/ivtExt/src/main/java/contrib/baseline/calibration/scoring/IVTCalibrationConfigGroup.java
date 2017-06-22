package contrib.baseline.calibration.scoring;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class IVTCalibrationConfigGroup extends ReflectiveConfigGroup {
    final static public String IVT_CALIBRATION = "ivt_calibration";

    public IVTCalibrationConfigGroup() {
        super(IVT_CALIBRATION);
    }

    final static public String ENABLED = "enabled";
    final static public String NUMBER_OF_DISTANCE_BINS = "number_of_distance_bins";
    final static public String MAXIMUM_DISTANCE = "maximum_distance";

    private boolean enabled = false;
    private int numberOfDistanceBins = 20;
    private double maximumDistance = 10000;

    @StringGetter(ENABLED)
    public boolean isEnabled() {
        return enabled;
    }

    @StringSetter(ENABLED)
    public void setEnabled(boolean enableCalibrationOutput) {
        this.enabled = enableCalibrationOutput;
    }

    @StringGetter(NUMBER_OF_DISTANCE_BINS)
    public int getNumberOfDistanceBins() {
        return numberOfDistanceBins;
    }

    @StringSetter(NUMBER_OF_DISTANCE_BINS)
    public void setNumberOfDistanceBins(int numberOfDistanceBins) {
        this.numberOfDistanceBins = numberOfDistanceBins;
    }

    @StringGetter(MAXIMUM_DISTANCE)
    public double getMaximumDistance() {
        return maximumDistance;
    }

    @StringSetter(MAXIMUM_DISTANCE)
    public void setMaximumDistance(double maximumDistance) {
        this.maximumDistance = maximumDistance;
    }
}
