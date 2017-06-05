package playground.sebhoerl.plcpc;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

public class ParallelLeastCostPathCalculatorListener implements MobsimAfterSimStepListener, IterationEndsListener, ShutdownListener {
    final private ParallelLeastCostPathCalculator calculator;

    public ParallelLeastCostPathCalculatorListener(ParallelLeastCostPathCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        calculator.notifyIterationEnds(event);
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        calculator.notifyShutdown(event);
    }

    @Override
    public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
        calculator.notifyMobsimAfterSimStep(e);
    }
}
