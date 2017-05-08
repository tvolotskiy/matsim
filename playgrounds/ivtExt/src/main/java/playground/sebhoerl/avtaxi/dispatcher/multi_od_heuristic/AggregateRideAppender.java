package playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic.aggregation.AggregatedRequest;

public interface AggregateRideAppender {
    void schedule(AggregatedRequest request, AVVehicle vehicle, double now);
    void update();
}
