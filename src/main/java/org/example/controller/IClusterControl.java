package org.example.controller;

import Demo.TaskResult;

public interface IClusterControl {
    // El Scheduler usa esto para mandar una tarea técnica a algún worker disponible
    TaskResult dispatchTask(String filePath, long startOffset, long endOffset);
}
