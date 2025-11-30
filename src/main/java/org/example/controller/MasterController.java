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

        // AUTO-DISCOVERY (Simplificado)
        // Aquí agregamos los workers conocidos.
        // En el laboratorio real, esto vendría de un archivo de configuración con las IPs de los 30 PCs.
        int[] puertos = {10000, 10001, 10002};
//        addWorker("SimpleWorker:tcp -h localhost -p 10000");
        for (int port : puertos) {
            // "SimpleWorker" es el nombre lógico que le pusimos en el adapter.add del Worker
            String proxy = "SimpleWorker:tcp -h localhost -p " + port;
            addWorker(proxy);
        }
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

        // PATRÓN: Load Balancing (Round Robin)
        // Selecciona el siguiente worker en la lista circularmente
        int index = roundRobinCounter.getAndIncrement() % workers.size();
        WorkerPrx selectedWorker = workers.get(Math.abs(index)); // Math.abs por si overflow

        System.out.println("[Controller] Despachando chunk a Worker[" + index + "]: "
                + startOffset + " - " + endOffset);

        try {
            // LLAMADA REMOTA (RPC)
            return selectedWorker.processDatagramLog(filePath, startOffset, endOffset);
        } catch (Exception e) {
            System.err.println("[Controller] Error comunicando con worker: " + e.getMessage());
            return new TaskResult(0, "Error", 0);
        }
    }
}
