package org.example.controller;

import Demo.TaskResult;
import Demo.WorkerPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MasterController implements IClusterControl {

    private final Communicator communicator;
    private final List<WorkerPrx> workers;
    private final AtomicInteger roundRobinCounter;

    public MasterController(Communicator communicator) {
        this.communicator = communicator;
        this.workers = new ArrayList<>();
        this.roundRobinCounter = new AtomicInteger(0);

        addWorker("SimpleWorker:tcp -h 10.147.17.104 -p 10000");
        addWorker("SimpleWorker:tcp -h 10.147.17.107 -p 10000");

    }

    private void addWorker(String proxyString) {
        try {
            ObjectPrx base = communicator.stringToProxy(proxyString);
            WorkerPrx worker = WorkerPrx.checkedCast(base);
            if (worker != null) {
                workers.add(worker);
                System.out.println("[Controller] Worker registrado y conectado: " + proxyString);
            }
        } catch (Exception e) {
            System.err.println("[Controller] Error conectando con worker " + proxyString);
        }
    }

    @Override
    public TaskResult dispatchTask(String filePath, long startOffset, long endOffset) {
        if (workers.isEmpty()) {
            System.err.println("[Controller] CRITICAL: No hay workers disponibles.");
            return new TaskResult(0, "Error", 0);
        }


        int index = roundRobinCounter.getAndIncrement() % workers.size();
        WorkerPrx selectedWorker = workers.get(Math.abs(index)); // Math.abs por si overflow

        System.out.println("[Controller] Despachando chunk a Worker[" + index + "]: "
                + startOffset + " - " + endOffset);

        try {

            return selectedWorker.processDatagramLog(filePath, startOffset, endOffset);
        } catch (Exception e) {
            System.err.println("[Controller] Error comunicando con worker: " + e.getMessage());
            return new TaskResult(0, "Error", 0);
        }
    }
}
