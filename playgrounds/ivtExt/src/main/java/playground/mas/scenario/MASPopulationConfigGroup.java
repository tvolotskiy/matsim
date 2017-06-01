package playground.mas.scenario;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class MASPopulationConfigGroup extends ReflectiveConfigGroup {
    final static public String EV_OWNERSHIP_RATE = "evOwnershipRate";
    final static public String CAR_OWNERSHIP_RATE = "carOwnershipRate";
    final static public String HOME_OFFICE_RATE = "homeOfficeRate";
    final static public String EBIKE_OWNERSHIP_RATE = "ebikeOwnershipRate";
    final static public String ORIGINAL_POPULATION_INPUT = "originalPopulationInput";
    final static public String ORIGINAL_NETWORK_INPUT = "originalNetworkInput";

    private Double evOwnershipRate = null;
    private Double carOwnershipRate = null;
    private Double homeOfficeRate = null;
    private Double ebikeOfficeRate = null;

    private String originalPopulationPath = null;
    private String originalNetworkPath = null;

    public MASPopulationConfigGroup() {
        super("adjust_mas");
    }

    @StringGetter(EV_OWNERSHIP_RATE)
    public Double getEvOwnershipRate() {
        return evOwnershipRate;
    }

    @StringSetter(EV_OWNERSHIP_RATE)
    public void setEvOwnershipRate(Double evOwnershipRate) {
        this.evOwnershipRate = evOwnershipRate;
    }

    @StringGetter(CAR_OWNERSHIP_RATE)
    public Double getCarOwnershipRate() {
        return carOwnershipRate;
    }

    @StringSetter(CAR_OWNERSHIP_RATE)
    public void setCarOwnershipRate(Double carOwnershipRate) {
        this.carOwnershipRate = carOwnershipRate;
    }

    @StringGetter(HOME_OFFICE_RATE)
    public Double getHomeOfficeRate() {
        return homeOfficeRate;
    }

    @StringSetter(HOME_OFFICE_RATE)
    public void setHomeOfficeRate(Double homeOfficeRate) {
        this.homeOfficeRate = homeOfficeRate;
    }

    @StringGetter(EBIKE_OWNERSHIP_RATE)
    public Double getEbikeOwnershipRate() {
        return ebikeOfficeRate;
    }

    @StringSetter(EBIKE_OWNERSHIP_RATE)
    public void setEbikeOwnershipRate(Double ebikeOfficeRate) {
        this.ebikeOfficeRate = ebikeOfficeRate;
    }

    @StringGetter(ORIGINAL_POPULATION_INPUT)
    public String getOriginalPopulationPath() {
        return originalPopulationPath;
    }

    @StringSetter(ORIGINAL_POPULATION_INPUT)
    public void setOriginalPopulationPath(String originalPopulationPath) {
        this.originalPopulationPath = originalPopulationPath;
    }

    @StringGetter(ORIGINAL_NETWORK_INPUT)
    public String getOriginalNetworkPath() {
        return originalNetworkPath;
    }

    @StringSetter(ORIGINAL_NETWORK_INPUT)
    public void setOriginalNetworkPath(String originalNetworkPath) {
        this.originalNetworkPath = originalNetworkPath;
    }
}
