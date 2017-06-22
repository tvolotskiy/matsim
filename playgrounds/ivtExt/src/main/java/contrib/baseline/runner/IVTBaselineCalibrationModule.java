package contrib.baseline.runner;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import contrib.baseline.calibration.scoring.CalibrationListener;
import contrib.baseline.calibration.scoring.IVTCalibrationConfigGroup;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class IVTBaselineCalibrationModule extends AbstractModule {
    @Override
    public void install() {
        IVTCalibrationConfigGroup calibrationConfig = (IVTCalibrationConfigGroup) getConfig().getModules().get(IVTCalibrationConfigGroup.IVT_CALIBRATION);

        if (calibrationConfig.isEnabled()) {
            addControlerListenerBinding().to(CalibrationListener.class);
        }
    }

    @Provides @Singleton
    CalibrationListener provideCalibrationListener(IVTCalibrationConfigGroup calibrationConfig, Config config, Population population, OutputDirectoryHierarchy hierarchy) {
        return new CalibrationListener(config.planCalcScore(), population, hierarchy, calibrationConfig.getNumberOfDistanceBins(), calibrationConfig.getMaximumDistance());
    }
}
