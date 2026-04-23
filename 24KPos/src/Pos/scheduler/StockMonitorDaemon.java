package Pos.scheduler;

import Pos.main.PosMain;

public class StockMonitorDaemon {

    private final PosMain posMain;
    private Thread thread;
    private volatile boolean running = true;

    public StockMonitorDaemon(PosMain posMain) {
        this.posMain = posMain;
    }

    public void start() {
        thread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(3 * 60 * 1000); // 3분

                    System.out.println("[자동저장] 실행");
                    posMain.save();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.out.println("[자동저장 오류] " + e.getMessage());
                }
            }
        });

        thread.setDaemon(true);
        thread.setName("pos-auto-save-daemon");
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }
}