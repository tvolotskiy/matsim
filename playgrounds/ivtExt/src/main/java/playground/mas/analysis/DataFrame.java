package playground.mas.analysis;

import playground.sebhoerl.av_paper.BinCalculator;

import java.util.*;

public class DataFrame {
    final private List<String> modes = Collections.unmodifiableList(Arrays.asList("car", "pt", "walk", "av_solo", "av_pool", "ev", "bike", "ebike"));
    final private List<String> ptModes = Collections.unmodifiableList(Arrays.asList("pt", "av_solo", "av_pool"));
    final private List<String> avModes = Collections.unmodifiableList(Arrays.asList("av_solo", "av_pool"));

    final public Map<String, List<Double>> insideOuterCordonArrivals;
    final public Map<String, List<Double>> insideOuterCordonDepartures;
    final public Map<String, List<Double>> insideInnerCordonArrivals;
    final public Map<String, List<Double>> insideInnerCordonDepartures;

    final public Map<String, List<Double>> arrivals;
    final public Map<String, List<Double>> departures;

    final public Collection<Double[]> scores;

    final public List<Double> outerCordonCrossings;
    final public List<Double> chargeableOuterCordonCrossings;

    final public List<Double> innerCordonDistance;
    final public List<Double> chargeableInnerCordonDistance;

    final public Map<String, List<Double>> vehicleDistances;
    final public Map<String, List<Double>> passengerDistances;
    final public Map<Long, List<Double>> distanceByPoolOccupancy;

    final public Map<String, List<Double>> insideOuterCordonVehicleDistances;
    final public Map<String, List<Double>> insideOuterCordonPassengerDistances;
    final public Map<Long, List<Double>> insideOuterCordonDistanceByPoolOccupancy;

    final public Map<String, List<Double>> insideInnerCordonVehicleDistances;
    final public Map<String, List<Double>> insideInnerCordonPassengerDistances;
    final public Map<Long, List<Double>> insideInnerCordonDistanceByPoolOccupancy;

    final public Map<String, List<List<Double>>> waitingTimes;
    final public Map<String, List<Double>> inactiveAVs;

    static public Double[] createScore(double x, double y, double score) {
        return new Double[] { x, y, score };
    }

    public DataFrame(BinCalculator binCalculator) {
        insideInnerCordonArrivals = new HashMap<>();
        insideInnerCordonDepartures = new HashMap<>();
        insideOuterCordonArrivals = new HashMap<>();
        insideOuterCordonDepartures = new HashMap<>();
        arrivals = new HashMap<>();
        departures = new HashMap<>();

        for (Map<String, List<Double>> item : Arrays.asList(insideInnerCordonArrivals, insideInnerCordonDepartures, insideOuterCordonArrivals, insideOuterCordonDepartures, arrivals, departures)) {
            for (String mode : modes) {
                item.put(mode, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
            }
        }

        scores = new LinkedList<>();

        outerCordonCrossings = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0));
        chargeableOuterCordonCrossings = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0));

        innerCordonDistance = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0));
        chargeableInnerCordonDistance = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0));


        vehicleDistances = new HashMap<>();
        passengerDistances = new HashMap<>();
        insideInnerCordonVehicleDistances = new HashMap<>();
        insideInnerCordonPassengerDistances = new HashMap<>();
        insideOuterCordonVehicleDistances = new HashMap<>();
        insideOuterCordonPassengerDistances = new HashMap<>();

        for (Map<String, List<Double>> item : Arrays.asList(vehicleDistances, passengerDistances, insideInnerCordonVehicleDistances, insideInnerCordonPassengerDistances, insideOuterCordonVehicleDistances, insideOuterCordonPassengerDistances)) {
            for (String mode : modes) {
                item.put(mode, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
            }
        }

        distanceByPoolOccupancy = new HashMap<>();
        insideInnerCordonDistanceByPoolOccupancy = new HashMap<>();
        insideOuterCordonDistanceByPoolOccupancy = new HashMap<>();

        for (long i = 1; i < 5; i++) {
            distanceByPoolOccupancy.put(i, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
            insideInnerCordonDistanceByPoolOccupancy.put(i, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
            insideOuterCordonDistanceByPoolOccupancy.put(i, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
        }

        waitingTimes = new HashMap<>();
        for (String mode : ptModes) {
            waitingTimes.put(mode, new ArrayList<>());
            for (int i = 0; i < binCalculator.getBins(); i++) waitingTimes.get(mode).add(new LinkedList<>());
        }

        inactiveAVs = new HashMap<>();
        for (String mode : avModes) {
            inactiveAVs.put(mode, new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0)));
        }
    }

    public static void increment(Map<String, List<Double>> map, String stringIndex, int index) {
        increment(map, stringIndex, index, 1.0);
    }

    public static void increment(Map<String, List<Double>> map, String stringIndex, int index, double amount) {
        if (map.containsKey(stringIndex)) {
            increment(map.get(stringIndex), index, amount);
        }
    }

    public static void increment(List<Double> list, int index) {
        increment(list, index, 1.0);
    }

    public static void increment(List<Double> list, int index, double amount) {
        list.set(index, list.get(index) + amount);
    }
}
