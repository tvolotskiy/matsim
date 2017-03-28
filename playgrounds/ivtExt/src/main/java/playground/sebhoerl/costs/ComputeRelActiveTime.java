package playground.sebhoerl.costs;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.*;
import java.util.*;

public class ComputeRelActiveTime {
    public static void main(String[] args) throws IOException {
        int fleetSizes[] = { 100, 250, 500, 750, 1000, 2000, 3000, 4000, 5000, 6000, 8000 };

        FileOutputStream stream = new FileOutputStream("/home/sebastian/utilization.csv");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

        String headers[] = {
                "FLEET_SIZE",
                "UTILZATION_MORNING",
                "UTILZATION_AFTERNOON",
                "UTILZATION_OFFPEAK",
                "WAITING_MEDIAN_MORNING",
                "WAITING_MEDIAN_AFTERNOON",
                "WAITING_MEDIAN_OFFPEAK",
                "WAITING_90P_MORNING",
                "WAITING_90P_AFTERNOON",
                "WAITING_90P_OFFPEAK",
                "WAITING_MEAN_MORNING",
                "WAITING_MEAN_AFTERNOON",
                "WAITING_MEAN_OFFPEAK"
        };

        writer.write(String.join(";", headers) + "\n");
        writer.flush();

        for (int fleetSize : fleetSizes) {
            LinkedList<String> elements = new LinkedList<>();
            elements.add(String.valueOf(fleetSize));

            for (double value : runUtilizationAnalysis("/home/sebastian/ant/analysis/data/single/events_" + fleetSize + "_nosub.xml.gz", fleetSize)) {
                elements.add(String.valueOf(value));
            }

            writer.write(String.join(";", elements) + "\n");
            writer.flush();
        }

        stream.close();
    }

    final static double MORNING_PEAK_START = 8 * 3600.0;
    final static double MORNING_PEAK_END = 9 * 3600.0;

    final static double AFTERNOON_PEAK_START = 17 * 3600.0;
    final static double AFTERNOON_PEAK_END = 18 * 3600.0;

    final static double OFFPEAK_START = 13 * 3600.0;
    final static double OFFPEAK_END = 14 * 3600.0;

    static public double[] runUtilizationAnalysis(String eventsSource, double numberOfVehicles) {
        RelActiveTimeHandler morningPeakUtilizationHandler = new RelActiveTimeHandler(MORNING_PEAK_START, MORNING_PEAK_END, numberOfVehicles);
        RelActiveTimeHandler afternoonPeakUtilizationHandler = new RelActiveTimeHandler(AFTERNOON_PEAK_START, AFTERNOON_PEAK_END, numberOfVehicles);
        RelActiveTimeHandler offpeakUtilizationHandler = new RelActiveTimeHandler(OFFPEAK_START, OFFPEAK_END, numberOfVehicles);

        WaitingTimeHandler morningPeakWaitingTimeHandler = new WaitingTimeHandler(MORNING_PEAK_START, MORNING_PEAK_END);
        WaitingTimeHandler afternoonPeakWaitingTimeHandler = new WaitingTimeHandler(AFTERNOON_PEAK_START, AFTERNOON_PEAK_END);
        WaitingTimeHandler offpeakWaitingTimeHandler = new WaitingTimeHandler(OFFPEAK_START, OFFPEAK_END);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(morningPeakUtilizationHandler);
        eventsManager.addHandler(afternoonPeakUtilizationHandler);
        eventsManager.addHandler(offpeakUtilizationHandler);
        eventsManager.addHandler(morningPeakWaitingTimeHandler);
        eventsManager.addHandler(afternoonPeakWaitingTimeHandler);
        eventsManager.addHandler(offpeakWaitingTimeHandler);

        new MatsimEventsReader(eventsManager).readFile(eventsSource);

        morningPeakUtilizationHandler.finish();
        afternoonPeakUtilizationHandler.finish();
        offpeakUtilizationHandler.finish();

        return new double[] {
                morningPeakUtilizationHandler.getUtilization(),
                afternoonPeakUtilizationHandler.getUtilization(),
                offpeakUtilizationHandler.getUtilization(),
                morningPeakWaitingTimeHandler.getStatistics().getPercentile(50),
                afternoonPeakWaitingTimeHandler.getStatistics().getPercentile(50),
                offpeakWaitingTimeHandler.getStatistics().getPercentile(50),
                morningPeakWaitingTimeHandler.getStatistics().getPercentile(90),
                afternoonPeakWaitingTimeHandler.getStatistics().getPercentile(90),
                offpeakWaitingTimeHandler.getStatistics().getPercentile(90),
                morningPeakWaitingTimeHandler.getStatistics().getMean(),
                afternoonPeakWaitingTimeHandler.getStatistics().getMean(),
                offpeakWaitingTimeHandler.getStatistics().getMean()
        };
    }

    static public class RelActiveTimeHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
        final private Map<Id<Person>, Double> customerStartTimes = new HashMap<>();

        final private double measurementStartTime;
        final private double measurementEndTime;
        final private double numberOfVehicles;

        private double totalIdleTime = 0.0;

        public double getTotalIdleTime() {
            return totalIdleTime;
        }

        public double getTotalTime() {
            return (measurementEndTime - measurementStartTime) * numberOfVehicles;
        }

        public double getUtilization() {
            return getTotalIdleTime() / getTotalTime();
        }

        public RelActiveTimeHandler(double measurementStartTime, double measurementEndTime, double numberOfVehicles) {
            this.measurementStartTime = measurementStartTime;
            this.measurementEndTime = measurementEndTime;
            this.numberOfVehicles = numberOfVehicles;
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            if (event.getActType().equals("AVDropoff")) {
                Double startTime = customerStartTimes.remove(event.getPersonId());
                Double endTime = event.getTime();

                if (startTime != null) {
                    boolean startTimeCovered = measurementStartTime <= startTime && startTime <= measurementEndTime;
                    boolean endTimeCovered = measurementStartTime <= endTime && endTime <= measurementEndTime;

                    if (startTimeCovered && endTimeCovered) {
                        totalIdleTime += endTime - startTime;
                    } else if (startTimeCovered && !endTimeCovered) {
                        totalIdleTime += measurementEndTime - startTime;
                    } else if (!startTimeCovered && endTimeCovered) {
                        totalIdleTime += endTime - measurementStartTime;
                    }
                }
            }
        }

        public void finish() {
            for (Id<Person> personId : customerStartTimes.keySet()) {
                handleEvent(new ActivityStartEvent(measurementEndTime, personId, null, null, "AVDropoff"));
            }
        }

        @Override
        public void handleEvent(ActivityEndEvent event) {
            if (event.getActType().equals("AVPickup")) {
                customerStartTimes.put(event.getPersonId(), event.getTime());
            }
        }

        @Override
        public void reset(int iteration) {}
    }

    static public class WaitingTimeHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {
        final private Map<Id<Person>, Double> waitingStartTimes = new HashMap<>();

        final private double measurementStartTime;
        final private double measurementEndTime;

        private DescriptiveStatistics measuredWaitingTimes = new DescriptiveStatistics();

        public DescriptiveStatistics getStatistics() {
            return measuredWaitingTimes;
        }

        public WaitingTimeHandler(double measurementStartTime, double measurementEndTime) {
            this.measurementStartTime = measurementStartTime;
            this.measurementEndTime = measurementEndTime;
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (event.getLegMode().equals("av")) {
                waitingStartTimes.put(event.getPersonId(), event.getTime());
            }
        }

        @Override
        public void handleEvent(PersonEntersVehicleEvent event) {
            Double startTime = waitingStartTimes.remove(event.getPersonId());
            Double endTime = event.getTime();

            if (startTime != null && measurementStartTime <= startTime && startTime <= measurementEndTime && measurementStartTime <= endTime && endTime <= measurementEndTime) {
                Double waitingTime = endTime - startTime;
                measuredWaitingTimes.addValue(waitingTime);
            }
        }

        @Override
        public void reset(int iteration) {}
    }
}
