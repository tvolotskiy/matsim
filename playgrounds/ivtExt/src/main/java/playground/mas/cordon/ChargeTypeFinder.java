package playground.mas.cordon;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.vehicles.Vehicle;
import playground.mas.MASAttributeUtils;
import playground.sebhoerl.avtaxi.framework.AVModule;

public class ChargeTypeFinder {
    final private Population population;

    public ChargeTypeFinder(Population population) {
        this.population = population;
    }

    public boolean isArtificialAgent(String agentId) {
        return agentId.startsWith("av_") || agentId.startsWith("pt_") || agentId.startsWith("bus_");
    }

    public boolean mayAffectDeparture(String mode, Id<Person> passengerId) {
        return (mode.equals(TransportMode.car) || mode.equals(AVModule.AV_MODE)) && !isArtificialAgent(passengerId.toString());
    }

    public ChargeType getChargeType(Person passenger, Id<Vehicle> vehicleId) {
        String plainVehicleId = vehicleId.toString();

        if (plainVehicleId.startsWith("av_")) {
            return plainVehicleId.contains("solo") ? ChargeType.AV_SOLO : ChargeType.AV_POOL;
        }

        return MASAttributeUtils.isEVUser(passenger) ? ChargeType.EV : ChargeType.CAR;
    }

    public ChargeType getChargeType(Id<Person> passengerId, Id<Vehicle> vehicleId) {
        return getChargeType(population.getPersons().get(passengerId), vehicleId);
    }
}
