package playground.sebhoerl.recharging_avs.tracker;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CSVConsumptionTracker implements ConsumptionTracker, IterationStartsListener, IterationEndsListener {
    private double currentTime = 0.0;

    private double currentTimeBasedConsumption;
    private double currentDistanceBasedConsumption;

    private List<Double> timeBasedConsumption = new LinkedList<>();
    private List<Double> distanceBasedConsumption = new LinkedList<>();

    @Override
    public void addDistanceBasedConsumption(double time, double consumption) {
        ensureTime(time);
        currentTimeBasedConsumption += consumption;
    }

    @Override
    public void addTimeBasedConsumption(double time, double consumption) {
        ensureTime(time);
        currentDistanceBasedConsumption += consumption;
    }

    private void ensureTime(double time) {
        if (time > currentTime) {
            currentTime = time;
            timeBasedConsumption.add(currentTimeBasedConsumption);
            distanceBasedConsumption.add(currentDistanceBasedConsumption);
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        currentTime = 0.0;
        currentDistanceBasedConsumption = 0.0;
        currentTimeBasedConsumption = 0.0;
        timeBasedConsumption.clear();
        distanceBasedConsumption.clear();
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        try {
            OutputStream stream = IOUtils.getOutputStream(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "consumption.csv"));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(stream)));

            writer.write("DISTANCE;TIME\n" + String.valueOf(distanceBasedConsumption.size()) + " " + String.valueOf(timeBasedConsumption.size()));

            Iterator<Double> distanceIterator = distanceBasedConsumption.iterator();
            Iterator<Double> timeIterator = timeBasedConsumption.iterator();

            while (distanceIterator.hasNext() && timeIterator.hasNext()) {
                writer.write(String.format("%f;%f\n", distanceIterator.next(), timeIterator.next()));
            }

            writer.flush();
            stream.flush();
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
