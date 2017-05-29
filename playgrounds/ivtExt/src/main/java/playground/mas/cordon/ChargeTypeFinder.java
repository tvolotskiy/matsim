package playground.mas.cordon;

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import playground.mas.MASModule;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.Collection;

public class ChargeTypeFinder {
    final private Collection<Id<Person>> evUserIds;

    public ChargeTypeFinder(@Named(MASModule.EV_USER_IDS) Collection<Id<Person>> evUserIds) {
        this.evUserIds = evUserIds;
    }

    public boolean isArtificialAgent(String agentId) {
        return agentId.startsWith("av_") || agentId.startsWith("pt_") || agentId.startsWith("bus_");
    }

    public boolean mayAffectDeparture(String mode, Id<Person> passengerId) {
        return (mode.equals(TransportMode.car) || mode.equals(AVModule.AV_MODE)) && !isArtificialAgent(passengerId.toString());
    }

    public ChargeType getChargeType(Id<Person> passengerId, Id<Vehicle> vehicleId) {
        String plainVehicleId = vehicleId.toString();

        if (plainVehicleId.startsWith("av_")) {
            return plainVehicleId.contains("solo") ? ChargeType.AV_SOLO : ChargeType.AV_POOL;
        }

        return evUserIds.contains(passengerId) ? ChargeType.EV : ChargeType.CAR;
    }
}
