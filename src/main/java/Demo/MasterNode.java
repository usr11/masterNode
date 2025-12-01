package Demo;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;
import org.example.controller.MasterController;
import org.example.facade.SystemFacade;
import org.example.scheduler.JobScheduler;

import java.io.File;
import java.util.Scanner;

public class MasterNode {
    public static void main(String[] args) {

        printBanner();

        try(Communicator communicator = Util.initialize(args, "config.master")) {

            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║            ✓ MASTER NODE INICIADO                         ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            System.out.println();

            // Construir sistema
            MasterController controller = new MasterController(communicator);
            JobScheduler scheduler = new JobScheduler(controller);
            SystemFacade facade = new SystemFacade(scheduler);

            // Menú de selección de archivo
            String selectedFile = showFileMenu();

            if (selectedFile == null) {
                System.out.println("[Master] Operación cancelada.");
                return;
            }

            // Validar archivo
            File file = new File(selectedFile);
            if (!file.exists()) {
                System.err.println("[Master] ERROR: Archivo no encontrado: " + selectedFile);
                return;
            }

            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║              INICIANDO ANÁLISIS DISTRIBUIDO                ║");
            System.out.println("╠════════════════════════════════════════════════════════════╣");
            System.out.println("  Archivo:  " + file.getName());
            System.out.println("  Tamaño:   " + String.format("%.2f MB", file.length() / 1024.0 / 1024.0));
            System.out.println("  Ruta:     " + file.getAbsolutePath());
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            System.out.println();

            // Iniciar análisis
            facade.startAnalysis(selectedFile);

        } catch (Exception e) {
            System.err.println("[Master] ERROR FATAL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MASTER NODE - MIO System                  ║");
        System.out.println("║         Sistema Distribuido de Análisis de Tráfico          ║");
        System.out.println("║                    Coordinator & Aggregator                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static String showFileMenu() {
        Scanner scanner = new Scanner(System.in);

        // Archivos predefinidos (ajusta las rutas según tu proyecto)
        String[] files = {
                "datagrams_1M.csv",      // Archivo 1: ~1 millón de registros
                "datagrams_test.csv",     // Archivo 2: ~10 millones
                "test_data.csv"     // Archivo 3: ~100 millones
        };

        String[] descriptions = {
                "~1 Millón de registros   (~85 MB)",
                "~10 Millones de registros (~850 MB)",
                "~100 Millones de registros (~8.5 GB)"
        };

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           SELECCIONE ARCHIVO PARA PROCESAR                 ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");

        for (int i = 0; i < files.length; i++) {
            File f = new File(files[i]);
            String status = f.exists() ? "✓" : "✗";
            System.out.println(String.format("  [%d] %s %s", (i + 1), status, descriptions[i]));
            System.out.println("      → " + files[i]);
        }

        System.out.println("  [0] Ingresar ruta manualmente");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.print("\nOpción: ");

        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consumir newline

            if (choice == 0) {
                System.out.print("Ingrese ruta completa del archivo: ");
                return scanner.nextLine().trim();
            } else if (choice >= 1 && choice <= files.length) {
                return files[choice - 1];
            } else {
                System.err.println("Opción inválida.");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Entrada inválida.");
            return null;
        }
    }
}