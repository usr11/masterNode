package org.example.scheduler;

public interface IJobManager {
    // La Facade usa esto para ordenar un trabajo de alto nivel
    void scheduleJob(String filePath);
}