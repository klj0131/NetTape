package team.jmworks.makertechno.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**Manage input to output in separated thread.*/
public class DecoupledPrinterCp<I> {
    private final ConcurrentLinkedQueue<I> messageQueue;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean stopped;
    private final PrinterTask task;
    private final Consumer<I> out;
    public DecoupledPrinterCp(Consumer<I> outputCallback, @NotNull Consumer<PrinterTask> executable, int miles) {
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.task = new PrinterTask(miles);
        this.out = outputCallback;
        this.isRunning = new AtomicBoolean(true);
        this.stopped = new AtomicBoolean(false);
        executable.accept(this.task);
    }
    public void start() {
        this.isRunning.set(true);
        this.tryCallUp();
    }
    public void print(I output){
        this.messageQueue.add(output);
        this.tryCallUp();
    }
    public void pause() {
        this.isRunning.set(false);
    }
    public void stop() {
        this.stopped.set(true);
    }
    private void callbackObj(I i){
        this.out.accept(i);
    }
    private void tryCallUp() {
        if (!isRunning.get() || !task.isWaiting.get()) return;
        task.callUp();
    }
    public class PrinterTask implements Runnable {
        private final int miles;
        private final AtomicBoolean isWaiting;
        private Thread thisThread;
        PrinterTask(int miles) {
            this.miles = miles;
            this.isWaiting = new AtomicBoolean(false);
        }
        @Override
        public void run() {
            thisThread = Thread.currentThread();
            while (!stopped.get()) doMessageCheckOutput();
        }
        private void doMessageCheckOutput() {
            if (!isRunning.get()) this.waitForCall();
            else {
                I message = messageQueue.poll();
                if (message != null) callbackObj(message);
                else this.waitForCall();
            }
        }
        private void waitForCall()  {
            this.isWaiting.set(true);
            try {
                Thread.sleep(miles);
            } catch (InterruptedException ignore) {}
        }
        private void callUp(){
            task.isWaiting.set(false);
            if (thisThread != null) thisThread.interrupt();
        }
    }
}