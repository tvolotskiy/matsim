package playground.mas.analysis;

import playground.sebhoerl.av_paper.BinCalculator;

import java.util.*;

public class DataFrame {
    final private List<String> modes = Collections.unmodifiableList(Arrays.asList("car", "pt", "walk", "av_solo", "av_pool"));
    final private List<String> ptModes = Collections.unmodifiableList(Arrays.asList("pt", "av_solo", "av_pool"));
    final private List<String> avModes = Collections.unmodifiableList(Arrays.asList("av_solo", "av_pool"));

    final public Map<String, List<Double>> insideArrivals;
    final public Map<String, List<Double>> insideDepartures;
    final public Map<String, List<Double>> insideEnroute;

    final public Map<String, List<Double>> outsideArrivals;
    final public Map<String, List<Double>> outsideDepartures;
    final public Map<String, List<Double>> outsideEnroute;

    final public Collection<Double[]> scores;
    final public List<Long> cordonCrossings;

    final public Map<String, List<Double>> vehicleDistances;
    final public Map<String, List<Double>> passengerDistances;
    final public Map<Long, List<Double>> distanceByPoolOccupancy;

    final public Map<String, List<List<Double>>> waitingTimes;
    final public Map<String, List<Double>> activeVehicles;

    static public Double[] createScore(double x, double y, double score) {
        return new Double[] { x, y, score };
    }

    public DataFrame(BinCalculator binCalculator) {
        insideArrivals = new HashMap<>();
        insideDepartures = new HashMap<>();
        insideEnroute = new HashMap<>();
        outsideArrivals = new HashMap<>();
        outsideDepartures = new HashMap<>();
        outsideEnroute = new HashMap<>();

        for (Map<String, List<Double>> item : Arrays.asList(insideArrivals, insideDepartures, insideEnroute, outsideArrivals, outsideDepartures, outsideEnroute)) {
            for (String mode : modes) {
                item.put(mode, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
            }
        }

        scores = new LinkedList<>();
        cordonCrossings = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0L));

        vehicleDistances = new HashMap<>();
        passengerDistances = new HashMap<>();

        for (Map<String, List<Double>> item : Arrays.asList(vehicleDistances, passengerDistances)) {
            for (String mode : modes) {
                item.put(mode, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
            }
        }

        distanceByPoolOccupancy = new HashMap<>();
        for (long i = 1; i < 5; i++) distanceByPoolOccupancy.put(i, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));

        waitingTimes = new HashMap<>();
        for (String mode : ptModes) {
            waitingTimes.put(mode, new ArrayList<>());
            for (int i = 0; i < binCalculator.getBins(); i++) waitingTimes.get(mode).add(new LinkedList<>());
        }

        activeVehicles = new HashMap<>();
        for (String mode : avModes) {
            activeVehicles.put(mode, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
        }
    }
}
