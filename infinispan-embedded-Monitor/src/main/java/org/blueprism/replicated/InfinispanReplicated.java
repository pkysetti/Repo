package org.blueprism.replicated;

import org.apache.log4j.PropertyConfigurator;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.Scanner;


public class InfinispanReplicated {
    final static Scanner scanner = new Scanner(System.in);

    final static String RESET = "\u001B[0m";
    final static String GREEN = "\u001B[92m";
    final static String WHITE = "\u001B[97m";

    /**
     * Main entry
     */
    public static void main(String[] args) throws Exception {
        String configuration = args.length > 0 ? args[0] : "infinispan.xml";

        configuration =System.getProperty("user.dir")+ File.separator +"config"
                + File.separator + configuration;

      String log4jConfigFile = System.getProperty("user.dir")+ File.separator +"config"
                + File.separator + "log4j.properties";
        PropertyConfigurator.configure(log4jConfigFile);

        System.out.println(">>>log4j"+log4jConfigFile);

         /*
        MyCacheManager.init(configuration);
        Controller.init(configuration);*/

        Controller.init(configuration);
        AnsiConsole.systemInstall();
        System.out.println("\n[?] Type '" + WHITE + "help" + RESET + "' to see all supported commands\n");
        while (Controller.isActive()) {
            try {
                AnsiConsole.out.print(GREEN + "[Command] >>> " + RESET);
                String[] params = scanner.nextLine().trim().split(" ");
                Controller.invoke(params);
            }
            catch (IllegalStateException | NoSuchElementException e) {
                System.out.println("System.in was closed; exiting");
                Controller.quit();
            }
            catch (Exception e) {
                MyLogger.exception(e);
            }
        }
        AnsiConsole.systemUninstall();
        MyCacheManager.destroy();
    }
}
