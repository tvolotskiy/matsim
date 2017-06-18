package contrib.baseline.modification;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

public class LiteratureScoring {
    // All values in CHF

    final double carConstant = -2.0 - 2.21; // 2 CHF Parking + 2 * 2min Walking
    final double ptConstant = 0.0;
    final double walkConstant = 0.0;
    final double transitWalkConstant = 0.0;
    final double bikeConstant = 0.0;

    final double carPerH = -23.29; // Source: SN 641 822a
    final double ptPerH = -14.43; // Source: SN 641 822a
    final double walkPerH = -33.20; // TODO: source somwhere H. Becker... it's not in the paper where it was supposed to be ;)
    final double transitWalkPerH = walkPerH;
    final double bikePerH = -26.0; // TODO: Crazy assumption with basis on vtpi.org

    final double ptWaitingPerH = -24.13; // Source: SN 641 822a
    final double ptPerLineSwitch = -2.45; // Source: SN 641 822a

    final double carPerKm = -0.176; // Source: Cost calculator & TCS (variable costs)
    final double ptPerKm = -0.53 * 0.5; // Source: Cost calculator (with 50% subsidies)
    final double walkPerKm = 0.0;
    final double transitwalkPerKm = 0.0;
    final double bikePerKm = 0.0;

    public void adjustScoring(PlanCalcScoreConfigGroup config) {
        config.setMarginalUtilityOfMoney(1.0);
        config.setMarginalUtlOfWaiting_utils_hr(0.0);
        config.setMarginalUtlOfWaitingPt_utils_hr(ptWaitingPerH - carPerH);
        config.setUtilityOfLineSwitch(ptPerLineSwitch);
        config.setPerforming_utils_hr(-carPerH);

        PlanCalcScoreConfigGroup.ModeParams modeParams;

        modeParams = config.getOrCreateModeParams(TransportMode.car);
        modeParams.setConstant(carConstant);
        modeParams.setMarginalUtilityOfTraveling(carPerH - carPerH);
        modeParams.setMonetaryDistanceRate(carPerKm / 1000.0);

        modeParams = config.getOrCreateModeParams(TransportMode.pt);
        modeParams.setConstant(ptConstant);
        modeParams.setMarginalUtilityOfTraveling(ptPerH - carPerH);
        modeParams.setMonetaryDistanceRate(ptPerKm / 1000.0);

        modeParams = config.getOrCreateModeParams(TransportMode.walk);
        modeParams.setConstant(walkConstant);
        modeParams.setMarginalUtilityOfTraveling(walkPerH - carPerH);
        modeParams.setMonetaryDistanceRate(walkPerKm / 1000.0);

        modeParams = config.getOrCreateModeParams(TransportMode.bike);
        modeParams.setConstant(bikeConstant);
        modeParams.setMarginalUtilityOfTraveling(bikePerH - carPerH);
        modeParams.setMonetaryDistanceRate(bikePerKm / 1000.0);

        modeParams = config.getOrCreateModeParams(TransportMode.transit_walk);
        modeParams.setConstant(transitWalkConstant);
        modeParams.setMarginalUtilityOfTraveling(transitWalkPerH - carPerH);
        modeParams.setMonetaryDistanceRate(transitwalkPerKm / 1000.0);
    }

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig(args[0]);
        new LiteratureScoring().adjustScoring(config.planCalcScore());
        new ConfigWriter(config).write(args[1]);
    }
}
