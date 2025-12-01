package org.example.scheduler;

import Demo.TaskResult;
import org.example.controller.IClusterControl;
import org.example.controller.MasterController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class JobScheduler implements IJobManager {

    private final IClusterControl controller;
    private static final long CHUNK_SIZE = 1024 * 1024 * 10; // 10 MB

    public JobScheduler(IClusterControl controller) {
        this.controller = controller;
    }

    @Override
    public void scheduleJob(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("[Scheduler] ✗ Archivo no existe: " + filePath);
            return;
        }

        long fileSize = file.length();
        int numChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║              PLANIFICACIÓN DE TRABAJO                      ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("  Tamaño archivo:  " + String.format("%,d bytes (%.2f MB)", fileSize, fileSize / 1024.0 / 1024.0));
        System.out.println("  Tamaño chunk:    " + String.format("%,d bytes (%.2f MB)", CHUNK_SIZE, CHUNK_SIZE / 1024.0 / 1024.0));
        System.out.println("  Total chunks:    " + numChunks);
        System.out.println("  Workers activos: " + getWorkerCount());
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<TaskResult>> futures = new ArrayList<>();

        long currentOffset = 0;
        long startTime = System.currentTimeMillis();
        int chunkNum = 1;

        // Disparar tareas
        System.out.println("[Scheduler] ═══ Despachando Chunks ═══");
        while (currentOffset < fileSize) {
            long start = currentOffset;
            long end = Math.min(currentOffset + CHUNK_SIZE, fileSize);

            System.out.println(String.format(
                    "[Scheduler] Chunk %d/%d agendado: [%,d - %,d]",
                    chunkNum++, numChunks, start, end
            ));

            Future<TaskResult> future = executor.submit(() ->
                    controller.dispatchTask(filePath, start, end)
            );

            futures.add(future);
            currentOffset = end;
        }

        // Recolectar resultados
        System.out.println("\n[Scheduler] ═══ Esperando Resultados ═══");
        System.out.println("[Scheduler] " + futures.size() + " tareas en proceso...");
        System.out.println();

        List<TaskResult> results = new ArrayList<>();
        for (Future<TaskResult> f : futures) {
            try {
                results.add(f.get());
            } catch (Exception e) {
                System.err.println("[Scheduler] Error obteniendo resultado: " + e.getMessage());
            }
        }

        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;

        printDetailedSummary(results, totalTime, fileSize);
    }

    private void printDetailedSummary(List<TaskResult> results, long totalTime, long fileSize) {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           REPORTE FINAL - ANÁLISIS COMPLETADO             ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println();

        // Métricas globales
        long totalWorkerTime = results.stream()
                .mapToLong(r -> r.executionTime)
                .sum();

        double avgMatchRate = results.stream()
                .mapToDouble(r -> r.value)
                .average()
                .orElse(0.0);

        long minTime = results.stream()
                .mapToLong(r -> r.executionTime)
                .min()
                .orElse(0);

        long maxTime = results.stream()
                .mapToLong(r -> r.executionTime)
                .max()
                .orElse(0);

        // ✅ NUEVAS MÉTRICAS DE VELOCIDAD
        int totalSpeedMeasurements = results.stream()
                .mapToInt(r -> r.speedCount)
                .sum();

        double globalAvgSpeed = 0.0;
        if (totalSpeedMeasurements > 0) {
            // Promedio ponderado de velocidades
            double weightedSum = 0;
            for (TaskResult r : results) {
                weightedSum += r.globalAvgSpeed * r.speedCount;
            }
            globalAvgSpeed = weightedSum / totalSpeedMeasurements;
        }

        System.out.println("  MÉTRICAS TEMPORALES:");
        System.out.println("  ─────────────────────────────────────────────────────────");
        System.out.println("  Tiempo total cluster:    " + String.format("%,d ms (%.2f s)", totalTime, totalTime / 1000.0));
        System.out.println("  Tiempo acumulado workers:" + String.format("%,d ms", totalWorkerTime));
        System.out.println("  Speedup real:            " + String.format("%.2fx", (double) totalWorkerTime / totalTime));
        System.out.println("  Chunk más rápido:        " + String.format("%,d ms", minTime));
        System.out.println("  Chunk más lento:         " + String.format("%,d ms", maxTime));
        System.out.println();

        System.out.println("  MÉTRICAS DE PROCESAMIENTO:");
        System.out.println("  ─────────────────────────────────────────────────────────");
        System.out.println("  Chunks procesados:       " + results.size());
        System.out.println("  Tasa de match promedio:  " + String.format("%.2f%%", avgMatchRate));
        System.out.println("  Throughput:              " + String.format("%.2f MB/s",
                (fileSize / 1024.0 / 1024.0) / (totalTime / 1000.0)));
        System.out.println();

        // ✅ NUEVAS MÉTRICAS DE VELOCIDAD
        System.out.println("  VELOCIDADES CALCULADAS:");
        System.out.println("  ─────────────────────────────────────────────────────────");
        System.out.println("  Total mediciones:        " + String.format("%,d", totalSpeedMeasurements));
        System.out.println("  Velocidad promedio:      " + String.format("%.2f km/h", globalAvgSpeed));
        System.out.println();

        System.out.println("  DETALLE POR CHUNK:");
        System.out.println("  ─────────────────────────────────────────────────────────");
        for (int i = 0; i < results.size(); i++) {
            TaskResult r = results.get(i);
            System.out.println(String.format(
                    "  Chunk %2d: %-12s | %,6d ms | Match: %5.1f%% | Speed: %,6d med. (%.2f km/h)",
                    i + 1,
                    r.workerName,
                    r.executionTime,
                    r.value,
                    r.speedCount,
                    r.globalAvgSpeed
            ));
        }

        System.out.println();

        // ✅ TOP ARCOS AGREGADOS DE TODOS LOS WORKERS
        printAggregatedTopArches(results);

        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║  ✓ ANÁLISIS COMPLETADO EXITOSAMENTE                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printAggregatedTopArches(List<TaskResult> results) {
        // Agregar estadísticas de arcos de todos los workers
        java.util.Map<String, ArchAggregation> archMap = new java.util.HashMap<>();

        for (TaskResult r : results) {
            if (r.topArches != null) {
                for (Demo.ArchSpeed arch : r.topArches) {
                    archMap.computeIfAbsent(arch.archKey, k -> new ArchAggregation())
                            .add(arch.avgSpeed, arch.count);
                }
            }
        }

        if (!archMap.isEmpty()) {
            System.out.println("  TOP 10 ARCOS MÁS TRANSITADOS (GLOBAL):");
            System.out.println("  ─────────────────────────────────────────────────────────");

            archMap.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().totalCount, a.getValue().totalCount))
                    .limit(10)
                    .forEach(entry -> {
                        ArchAggregation agg = entry.getValue();
                        System.out.println(String.format(
                                "    %-20s: %,6d mediciones | %.2f km/h promedio",
                                entry.getKey(),
                                agg.totalCount,
                                agg.getWeightedAverage()
                        ));
                    });

            System.out.println();
        }
    }

    // Clase helper para agregar velocidades
    private static class ArchAggregation {
        double weightedSum = 0;
        int totalCount = 0;

        void add(double avgSpeed, int count) {
            weightedSum += avgSpeed * count;
            totalCount += count;
        }

        double getWeightedAverage() {
            return totalCount > 0 ? weightedSum / totalCount : 0.0;
        }
    }

    private int getWorkerCount() {
        // Método helper para obtener cantidad de workers
        // Si el controller es MasterController, retorna el count real
        try {
            // Usar reflexión para obtener el método sin castear
            java.lang.reflect.Method method = controller.getClass().getMethod("getWorkerCount");
            return (int) method.invoke(controller);
        } catch (Exception e) {
            // Si falla, asumimos 1 worker
            return 1;
        }
    }
}