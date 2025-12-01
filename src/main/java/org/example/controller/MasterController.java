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

        System.out.println("[Controller] ═══ Descubriendo Workers ═══");

        // ✅ Configuración de workers
        // Aquí defines tus workers (localhost o IPs remotas)
        String[] workerProxies = {
                "SimpleWorker:tcp -h localhost -p 10000",
                // "SimpleWorker:tcp -h localhost -p 10001",  // Descomenta para más workers
                // "SimpleWorker:tcp -h localhost -p 10002",
                // "SimpleWorker:tcp -h 10.147.17.107 -p 10000",  // Worker remoto
        };

        for (String proxy : workerProxies) {
            addWorker(proxy);
        }

        System.out.println("[Controller] ─────────────────────────────");
        System.out.println("[Controller] Workers disponibles: " + workers.size());
        System.out.println("[Controller] ═════════════════════════════");
        System.out.println();

        if (workers.isEmpty()) {
            System.err.println("╔════════════════════════════════════════════════════════════╗");
            System.err.println("║  ⚠️  ADVERTENCIA: NO HAY WORKERS DISPONIBLES              ║");
            System.err.println("╠════════════════════════════════════════════════════════════╣");
            System.err.println("  Asegúrate de iniciar los workers ANTES del master.");
            System.err.println("  Comando: java Demo.WorkerNode config.worker");
            System.err.println("╚════════════════════════════════════════════════════════════╝");
        }
    }

    private void addWorker(String proxyString) {
        try {
            System.out.print("[Controller] Conectando a " + proxyString + " ... ");

            ObjectPrx base = communicator.stringToProxy(proxyString);
            WorkerPrx worker = WorkerPrx.checkedCast(base);

            if (worker != null) {
                workers.add(worker);
                System.out.println("✓ OK");
            } else {
                System.out.println("✗ No responde");
            }
        } catch (Exception e) {
            System.out.println("✗ ERROR: " + e.getMessage());
        }
    }

    @Override
    public TaskResult dispatchTask(String filePath, long startOffset, long endOffset) {
        if (workers.isEmpty()) {
            System.err.println("[Controller] ✗ ERROR: No hay workers disponibles.");
            // Crear TaskResult vacío con el nuevo constructor
            return new TaskResult(
                    0.0,              // value (match rate)
                    "NoWorkers",      // workerName
                    0L,               // executionTime
                    0.0,              // globalAvgSpeed
                    0,                // speedCount
                    new Demo.ArchSpeed[0]  // topArches (array vacío)
            );
        }

        // Round Robin
        int index = Math.abs(roundRobinCounter.getAndIncrement() % workers.size());
        WorkerPrx selectedWorker = workers.get(index);

        System.out.println(String.format(
                "[Controller] → Worker[%d] procesando chunk [%,d - %,d] (%s)",
                index,
                startOffset,
                endOffset,
                formatBytes(endOffset - startOffset)
        ));

        try {
            long start = System.currentTimeMillis();
            TaskResult result = selectedWorker.processDatagramLog(filePath, startOffset, endOffset);
            long netTime = System.currentTimeMillis() - start;

            System.out.println(String.format(
                    "[Controller] ← Worker[%d] completó en %,d ms (%.1f%% match, %.2f km/h avg) [Red: %,d ms]",
                    index,
                    result.executionTime,
                    result.value,
                    result.globalAvgSpeed,
                    netTime
            ));

            return result;

        } catch (Exception e) {
            System.err.println("[Controller] ✗ Error Worker[" + index + "]: " + e.getMessage());
            // Retornar TaskResult de error con el nuevo constructor
            return new TaskResult(
                    0.0,
                    "Error-Worker" + index,
                    0L,
                    0.0,
                    0,
                    new Demo.ArchSpeed[0]
            );
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
    }

    public int getWorkerCount() {
        return workers.size();
    }
}