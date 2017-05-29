package playground.mas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import playground.sebhoerl.avtaxi.data.AVOperator;

import java.util.Map;

@Singleton
public class MASConsistencyListener implements IterationStartsListener {
    final private Map<Id<AVOperator>, AVOperator> operators;

    @Inject
    public MASConsistencyListener(Map<Id<AVOperator>, AVOperator> operators) {
        this.operators = operators;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (!operators.containsKey(Id.create(MASModule.AV_SOLO_OPERATOR, AVOperator.class))) {
            throw new RuntimeException();
        }

        if (!operators.containsKey(Id.create(MASModule.AV_POOL_OPERATOR, AVOperator.class))) {
            throw new RuntimeException();
        }
    }
}
