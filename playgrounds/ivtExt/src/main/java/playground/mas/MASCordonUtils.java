package playground.mas;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import playground.sebhoerl.avtaxi.data.AVOperator;

import java.util.Collection;

public class MASCordonUtils {
    static public boolean isChargeableOperator(Id<Vehicle> vehicleId, Collection<Id<AVOperator>> chargeableOperators) {
        String stringId = vehicleId.toString();

        if (stringId.startsWith("av_")) {
            for (Id<AVOperator> operatorId : chargeableOperators) {
                if (stringId.startsWith("av_" + operatorId.toString())) {
                    return true;
                }
            }
        }

        return false;
    }
}
