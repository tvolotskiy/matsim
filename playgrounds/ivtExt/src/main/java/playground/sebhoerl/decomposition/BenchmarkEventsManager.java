package playground.sebhoerl.decomposition;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

public class BenchmarkEventsManager implements EventsManager {
	final private Map<Class<?>, AtomicInteger> counts = new HashMap<>();
	
	public BenchmarkEventsManager() {
		counts.put(LinkEnterEvent.class, new AtomicInteger(0));
		counts.put(LinkLeaveEvent.class, new AtomicInteger(0));
		counts.put(PersonStuckEvent.class, new AtomicInteger(0));
	}
	
	@Override
	public void processEvent(Event event) {
		AtomicInteger count = counts.get(event.getClass());
		if (count != null) count.incrementAndGet();
	}
	
	public Map<Class<?>, AtomicInteger> getCounts() {
		return counts;
	}
	
	public void printCounts() {
		for (Map.Entry<Class<?>, AtomicInteger> entry : counts.entrySet()) {
			System.out.println(String.format(entry.getKey().toString() + ": " + entry.getValue().toString()));
		}
	}
	
	public void reset() {
		for (Class<?> key : counts.keySet()) {
			counts.put(key, new AtomicInteger(0));
		}
	}

	@Override
	public void addHandler(EventHandler handler) {}

	@Override
	public void removeHandler(EventHandler handler) {}

	@Override
	public void resetHandlers(int iteration) {}

	@Override
	public void initProcessing() {}

	@Override
	public void afterSimStep(double time) {}

	@Override
	public void finishProcessing() {}
}
