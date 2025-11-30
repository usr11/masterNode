module Demo {
    struct TaskResult {
        double value;
        string workerName;
        long executionTime;
    };
    interface Worker {
        TaskResult processDatagramLog(string filePath, long startOffset, long endOffset);
    };
};