package org.example.facade;

import org.example.scheduler.IJobManager;


public class SystemFacade {

    private final IJobManager jobManager;

    public SystemFacade(IJobManager jobManager) {
        this.jobManager = jobManager;
    }

    /**
     * Interfaz Lollipop expuesta al usuario/cronograma
     */
    public void startAnalysis(String filePath) {
        System.out.println("[Facade] Solicitud recibida: Analizar " + filePath);
        // Delega inmediatamente al experto (Scheduler)
        jobManager.scheduleJob(filePath);
    }

}
