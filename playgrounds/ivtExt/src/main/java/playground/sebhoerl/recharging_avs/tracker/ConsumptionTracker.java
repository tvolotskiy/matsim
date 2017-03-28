package playground.sebhoerl.recharging_avs.tracker;

public interface ConsumptionTracker {
    void addDistanceBasedConsumption(double time, double consumption);
    void addTimeBasedConsumption(double time, double consumption);
}
