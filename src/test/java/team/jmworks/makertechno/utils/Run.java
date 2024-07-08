package team.jmworks.makertechno.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**This is an example for DecoupledPrinter*/
public class Run {
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 16, 1000, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        DecoupledPrinterCp<String> printer = new DecoupledPrinterCp<>(System.out::println, executor::execute, 10000);

        printer.start();

        executor.execute(() -> {
            int i = 0;
            while (i < 100){
                printer.print("Core1-access: " + i++);
            }
            System.out.println("Task1 send done");
        });
        executor.execute(() -> {
            int i = 0;
            while (i < 100){
                printer.print("Core2-access: " + i++);
            }
            System.out.println("Task2 send done");
        });
        executor.execute(() -> {
            int i = 0;
            while (i < 100){
                printer.print("Core3-access: " + i++);
            }
            System.out.println("Task3 send done");
        });
        Thread.sleep(100);
        System.exit(0);
    }
}
