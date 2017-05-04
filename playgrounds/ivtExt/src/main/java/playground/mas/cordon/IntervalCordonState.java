package playground.mas.cordon;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IntervalCordonState implements CordonState {
    final private Set<Tuple<Double, Double>> intervals = new HashSet<>();

    public void addInterval(double startTime, double endTime) {
        intervals.add(new Tuple<>(startTime, endTime));
    }

    public Collection<Tuple<Double, Double>> getIntervals() {
        return Collections.unmodifiableCollection(intervals);
    }

    @Override
    public boolean isCordonActive(double time) {
        for (Tuple<Double, Double> interval : intervals) {
            if (interval.getFirst() <= time && time <= interval.getSecond()) {
                return true;
            }
        }

        return false;
    }

    static public class Reader {
        final private IntervalCordonState state;

        public Reader(IntervalCordonState state) {
            this.state = state;
        }

        public void read(String intervalInformation) {
            String[] stringIntervals = intervalInformation.split(",");

            for (String stringInterval : stringIntervals) {
                String[] parts = stringInterval.trim().split("-");

                String fromString = parts[0].trim();
                String toString = parts[1].trim();

                double fromTime = Time.parseTime(fromString);
                double toTime = Time.parseTime(toString);

                state.addInterval(fromTime, toTime);
            }
        }
    }
}
