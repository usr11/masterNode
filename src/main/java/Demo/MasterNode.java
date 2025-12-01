package Demo;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;
import org.example.controller.MasterController;
import org.example.facade.SystemFacade;
import org.example.scheduler.JobScheduler;

public class MasterNode {
    public static void main(String[] args) {

        try(Communicator communicator = Util.initialize(args, "config.master")) {

            System.out.println("--- INICIANDO MASTER NODE ---");


            MasterController controller = new MasterController(communicator);


            JobScheduler scheduler = new JobScheduler(controller);


            SystemFacade facade = new SystemFacade(scheduler);

             facade.startAnalysis("test_data.csv");

        }
    }
}