package playground.sebhoerl.plcpc;

import org.apache.log4j.Logger;
import org.matsim.core.router.util.LeastCostPathCalculator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ParallelLeastCostPathCalculatorWorker extends Thread implements LeastCostPathCalculatorWorker {
    final private List<ParallelLeastCostPathCalculatorTask> pending = Collections.synchronizedList(new LinkedList<>());
    final private LeastCostPathCalculator router;

    static private long runningWorkerId = 0;
    final private long workerId = runningWorkerId++;

    final private Object waitLock = new Object();
    private boolean running = true;

    static private Logger logger = Logger.getLogger(ParallelLeastCostPathCalculatorWorker.class);

    public ParallelLeastCostPathCalculatorWorker(LeastCostPathCalculator router) {
        this.router = router;
    }

    @Override
    public void addTask(ParallelLeastCostPathCalculatorTask task) {
        pending.add(task);

        synchronized (waitLock) {
            waitLock.notify();
        }
    }

    @Override
    public boolean isDone() {
        return pending.size() == 0;
    }

    @Override
    public void finishIteration() {
        synchronized (waitLock) {
            waitLock.notify();
        }
    }

    @Override
    public void run() {
        logger.info("Startup ParallelLeastCostPathCalculatorWorker #" + workerId);

        try {
            while (running) {
                synchronized (waitLock) {
                    while (pending.size() > 0) {
                        ParallelLeastCostPathCalculatorTask task = pending.remove(0);
                        task.result = router.calcLeastCostPath(task.fromNode, task.toNode, task.time, task.person, task.vehicle);
                    }

                    waitLock.wait();
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Routing worker has been interrupted.");
        }

        logger.info("Shutdown ParallelLeastCostPathCalculatorWorker #" + workerId);
    }

    public void terminate() {
        running = false;

        synchronized (waitLock) {
            waitLock.notify();
        }
    }
}
