package org.example.scheduler;

import Demo.TaskResult;
import org.example.controller.IClusterControl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class JobScheduler implements IJobManager {

    private final IClusterControl controller;

    // Tamaño del chunk (10MB está bien)
    private static final long CHUNK_SIZE = 1024 * 1024 * 10;

    public JobScheduler(IClusterControl controller) {
        this.controller = controller;
    }

    @Override
    public void scheduleJob(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("[Scheduler] Archivo no existe.");
            return;
        }

        long fileSize = file.length();
        System.out.println("[Scheduler] Iniciando Job Paralelo. Tamaño: " + (fileSize / 1024) + " KB");

        // 1. Crear un Pool de Hilos en el Master para gestionar las peticiones
        // Esto permite mandar múltiples órdenes a la vez sin bloquearse
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<TaskResult>> futuros = new ArrayList<>();

        long currentOffset = 0;
        long startTime = System.currentTimeMillis();

        // 2. DISPARAR PETICIONES (NON-BLOCKING)
        while (currentOffset < fileSize) {
            long start = currentOffset;
            long end = Math.min(currentOffset + CHUNK_SIZE, fileSize);

            // Enviamos la tarea a un hilo secundario
            System.out.println("[Scheduler] Agendando Chunk: " + start + " - " + end);

            Future<TaskResult> futuro = executor.submit(() -> {
                // Esta línea se ejecutará en paralelo
                return controller.dispatchTask(filePath, start, end);
            });

            futuros.add(futuro);
            currentOffset = end;
        }

        // 3. RECOLECTAR RESULTADOS (Barrier)
        // Aquí sí esperamos a que todos terminen
        System.out.println("[Scheduler] Todas las tareas enviadas. Esperando respuestas...");
        List<TaskResult> resultados = new ArrayList<>();

        for (Future<TaskResult> f : futuros) {
            try {
                // .get() bloquea hasta que ESE worker específico termine
                resultados.add(f.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;
        printSummary(resultados, totalTime);
    }

    private void printSummary(List<TaskResult> results, long totalTime) {
        System.out.println("\n=== REPORTE FINAL ===");
        System.out.println("Tiempo Total Cluster: " + totalTime + " ms");
        for (TaskResult r : results) {
            System.out.println("Worker: " + r.workerName + " | Tiempo: " + r.executionTime + "ms");
        }
    }
}