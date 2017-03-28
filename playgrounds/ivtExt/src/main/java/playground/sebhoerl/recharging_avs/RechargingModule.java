package playground.sebhoerl.recharging_avs;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.jfree.data.io.CSV;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.recharging_avs.calculators.ChargeCalculator;
import playground.sebhoerl.recharging_avs.calculators.StaticChargeCalculator;
import playground.sebhoerl.recharging_avs.logic.RechargingDispatcher;
import playground.sebhoerl.recharging_avs.tracker.CSVConsumptionTracker;
import playground.sebhoerl.recharging_avs.tracker.ConsumptionTracker;
import playground.sebhoerl.recharging_avs.tracker.NullConsumptionTracker;

public class RechargingModule extends AbstractModule {
    @Override
    public void install() {
        bind(ChargeCalculator.class).to(StaticChargeCalculator.class).asEagerSingleton();
        AVUtils.registerDispatcherFactory(binder(), "Recharging", RechargingDispatcher.Factory.class);

        RechargingConfig config = (RechargingConfig)getConfig().getModules().get(RechargingConfig.RECHARGING);

        if (config.getTrackConsumption()) {
            bind(ConsumptionTracker.class).to(CSVConsumptionTracker.class);
            addControlerListenerBinding().to(CSVConsumptionTracker.class);
        } else {
            bind(ConsumptionTracker.class).toInstance(new NullConsumptionTracker());
        }
    }
}
