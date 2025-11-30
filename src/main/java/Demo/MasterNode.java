package Demo;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;
import org.example.controller.MasterController;
import org.example.facade.SystemFacade;
import org.example.scheduler.JobScheduler;

public class MasterNode {
    public static void main(String[] args) {
        // Inicializamos ICE
        try(Communicator communicator = Util.initialize(args, "config.master")) {

            System.out.println("--- INICIANDO MASTER NODE ---");

            // 1. Crear Componentes (De abajo hacia arriba)

            // Capa Inferior: Controller (Sabe hablar ICE)
            MasterController controller = new MasterController(communicator);

            // Capa Media: Scheduler (Sabe dividir tareas)
            JobScheduler scheduler = new JobScheduler(controller);

            // Capa Superior: Facade (Sabe recibir órdenes)
            SystemFacade facade = new SystemFacade(scheduler);

            // 2. Ejecutar el Sistema
            // Nota: Para probar, asegúrate de tener un archivo dummy o real en esa ruta
            // Si el archivo no existe, crea uno vacío llamado 'test_data.csv' en la raíz del proyecto
            facade.startAnalysis("datagrams_test.csv");

            // Mantener vivo si fuera un servidor persistente,
            // pero como es un Job Batch, terminamos cuando acabe la función.
        }
    }
}