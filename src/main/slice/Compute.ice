module Demo {

    // Estructura para estadísticas de un arco
    struct ArchSpeed {
        string archKey;      // "lineId-orientation-variant"
        double avgSpeed;     // Velocidad promedio en km/h
        int count;           // Cantidad de mediciones
    };

    // Secuencia de arcos
    sequence<ArchSpeed> ArchSpeedList;

    // Resultado de una tarea
    struct TaskResult {
        double value;              // Tasa de match (%)
        string workerName;         // Nombre del worker
        long executionTime;        // Tiempo de ejecución (ms)
        double globalAvgSpeed;     // Velocidad promedio global del chunk (km/h)
        int speedCount;            // Cantidad de velocidades calculadas
        ArchSpeedList topArches;   // Top 5 arcos más transitados
    };

    interface Worker {
        TaskResult processDatagramLog(string filePath, long startOffset, long endOffset);
    };
};