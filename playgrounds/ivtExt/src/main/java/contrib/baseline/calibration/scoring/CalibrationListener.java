package contrib.baseline.calibration.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class CalibrationListener implements IterationEndsListener, ShutdownListener {
    final static private List<String> modes = Arrays.asList(TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk);

    final private PlanCalcScoreConfigGroup scoringConfig;
    final private Population population;
    final private OutputDirectoryHierarchy hierarchy;

    final private BestPlanSelector bestPlanSelector = new BestPlanSelector();

    final private double maximumDistance;
    final private int numberOfDistanceBins;

    final private List<List<String>> data = new LinkedList<>();

    public CalibrationListener(PlanCalcScoreConfigGroup scoringConfig, Population population, OutputDirectoryHierarchy hierarchy, int numberOfDistanceBins, double maximumDistance) {
        this.scoringConfig = scoringConfig;
        this.population = population;
        this.hierarchy = hierarchy;

        this.maximumDistance = maximumDistance;
        this.numberOfDistanceBins = numberOfDistanceBins;

        List<String> header = new LinkedList<>();
        applyHeader(header);
        data.add(header);
    }

    private void applyParameters(List<String> row) {
        for (String mode : modes) {
            PlanCalcScoreConfigGroup.ModeParams params = this.scoringConfig.getOrCreateModeParams(mode);
            row.add(String.valueOf(params.getConstant()));
        }

        for (String mode : modes) {
            PlanCalcScoreConfigGroup.ModeParams params = this.scoringConfig.getOrCreateModeParams(mode);
            row.add(String.valueOf(params.getMarginalUtilityOfTraveling()));
        }

        for (String mode : modes) {
            PlanCalcScoreConfigGroup.ModeParams params = this.scoringConfig.getOrCreateModeParams(mode);
            row.add(String.valueOf(params.getMonetaryDistanceRate()));
        }
    }

    private int getDistanceCategory(double distance) {
        return (int) Math.min(Math.floor(distance / maximumDistance), numberOfDistanceBins - 1);
    }

    private void applyStatistics(List<String> row) {
        Map<String, AtomicLong> tripCount = new HashMap<>();
        for (String mode : modes) tripCount.put(mode, new AtomicLong());

        Map<String, List<AtomicLong>> distanceCount = new HashMap<>();
        for (String mode : modes) {
            List<AtomicLong> bins = new LinkedList<>();
            for (int i = 0; i < numberOfDistanceBins; i++) bins.add(new AtomicLong());
            distanceCount.put(mode, bins);
        }

        for (Person person : population.getPersons().values()) {
            Plan plan = (Plan) bestPlanSelector.selectPlan(person);

            boolean isOngoing = false;
            double currentDistance = Double.NaN;
            String currentMode = null;

            for (PlanElement element : plan.getPlanElements()) {
                if (element instanceof Leg) {
                    Leg leg = (Leg) element;

                    if (isOngoing) {
                        currentDistance += leg.getRoute().getDistance();
                    } else {
                        isOngoing = true;
                        currentDistance = leg.getRoute().getDistance();
                        currentMode = leg.getMode();
                        if (currentMode.contains("transit") || currentMode.contains("pt")) currentMode = "pt";
                    }
                } else if (element instanceof Activity) {
                    Activity activity = (Activity) element;

                    if (isOngoing && !activity.getType().contains("interaction")) {
                        isOngoing = false;

                        tripCount.get(currentMode).incrementAndGet();
                        distanceCount.get(currentMode).get(getDistanceCategory(currentDistance)).incrementAndGet();
                    }
                }
            }
        }

        double totalTrips = tripCount.values().stream().mapToDouble(d -> d.doubleValue()).sum();

        for (String mode : modes) {
            row.add(String.valueOf(tripCount.get(mode).doubleValue() / totalTrips));
        }

        for (String mode : modes) {
            for (int i = 0; i < numberOfDistanceBins; i++) {
                row.add(String.valueOf(distanceCount.get(mode).get(i).longValue()));
            }
        }
    }

    private void applyHeader(List<String> row) {
        row.add("iteration");

        for (String mode : modes) {
            row.add("constant_" + mode);
        }

        for (String mode : modes) {
            row.add("vot_" + mode);
        }

        for (String mode : modes) {
            row.add("cost_" + mode);
        }

        for (String mode : modes) {
            row.add("share_" + mode);
        }

        for (String mode : modes) {
            for (int i = 0; i < numberOfDistanceBins; i++) {
                row.add("distances_" + mode + "_bin_" + i);
            }
        }
    }

    private void write(String outputPath) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputPath));

            for (List<String> row : data) {
                writer.write(String.join(";", row) + "\n");
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        List<String> row = new LinkedList<>();

        row.add(String.valueOf(event.getIteration()));
        applyParameters(row);
        applyStatistics(row);

        data.add(row);

        write(hierarchy.getIterationFilename(event.getIteration(), "calibration.csv"));
    }


    @Override
    public void notifyShutdown(ShutdownEvent event) {
        write(hierarchy.getOutputFilename("calibration.csv"));
    }
}
