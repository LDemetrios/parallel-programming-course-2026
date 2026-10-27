package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/* Этап 1: планировщик, который ведёт настоящие потоки строго по одному (передача хода через семафоры).
 * Этап 2: обнаружение deadlock в главном цикле и корректное завершение застрявших потоков.
 * Внутреннее устройство (свои классы, семафоры, вспомогательные методы для Shared и SchedLock) придумываете сами. */
public final class Scheduler {
    /**
     * Если true, каждая операция с общими данными добавляет строку в log. Выставляет runner.
     */
    boolean logging;
    /**
     * Лог прогона: строки "<поток>  <операция>", например "A  lock mu", "A  balance -> 100".
     */
    final List<String> log = new ArrayList<>();
    /**
     * null, если deadlock не было; иначе "held={a:A, b:B}" (этап 2).
     */
    String deadlock;

    private static final ThreadLocal<Worker> CURRENT = new ThreadLocal<>();

    private final List<Worker> workers = new ArrayList<>();

    final List<SchedLock> locks = new ArrayList<>();

    private final Semaphore yielded = new Semaphore(0);

    private final StringBuilder trace = new StringBuilder();

    private volatile boolean aborted;

    private boolean started;

    private static final class Cancelled extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    static final class Worker {
        final Scheduler scheduler;
        final int id;
        final Semaphore turn = new Semaphore(0);
        Thread thread;
        BooleanSupplier ready = () -> true;
        boolean touched = true;
        boolean done;
        Throwable failure;

        Worker(Scheduler scheduler, int id) {
            this.scheduler = scheduler;
            this.id = id;
        }
    }

    static Worker current() {
        Worker w = CURRENT.get();
        if (w == null) {
            throw new IllegalStateException("Operation outside scheduler worker");
        }
        if (w.scheduler.aborted) {
            throw new Cancelled();
        }
        return w;
    }

    static void operation(BooleanSupplier ready) {
        Worker w = current();
        w.ready = ready;
        if (w.touched || !ready.getAsBoolean()) {
            w.touched = false;
            w.scheduler.yielded.release();
            w.turn.acquireUninterruptibly();
            if (w.scheduler.aborted) {
                throw new Cancelled();
            }
        }
        w.ready = () -> true;
        w.touched = true;
    }

    static void operation() {
        operation(() -> true);
    }

    static void record(String message) {
        Worker w = current();
        if (w.scheduler.logging) {
            w.scheduler.log.add((char) ('A' + w.id) + "  " + message);
        }
    }

    /**
     * Зарегистрировать поток. Потоки получают буквы A, B, C… в порядке вызова spawn.
     */
    public void spawn(Runnable body) {
        if (started) {
            throw new IllegalStateException("Run already started");
        }
        Worker w = new Worker(this, workers.size());
        w.thread = new Thread(
                () -> {
                    CURRENT.set(w);
                    try {
                        body.run();
                    } catch (Cancelled ignored) {
                    } catch (Throwable e) {
                        w.failure = e;
                    } finally {
                        w.done = true;
                        CURRENT.remove();
                        yielded.release();
                    }
                },
                "scheduled-" + (char) ('A' + w.id)
        );
        workers.add(w);
    }

    /**
     * Запустить потоки и вести их по одному: на каждой паузе спросить strategy.pick(enabled, last).
     */
    public void run(Strategy strategy) {
        if (started) {
            throw new IllegalStateException("Scheduler is single-use");
        }
        started = true;
        try {
            for (Worker w : workers) {
                w.thread.start();
                yielded.acquireUninterruptibly();
            }
            int last = -1;
            while (true) {
                workers.stream()
                        .filter(w -> w.failure != null)
                        .findFirst()
                        .ifPresent(w -> {
                            throw new IllegalStateException("Worker failed", w.failure);
                        });
                List<Worker> alive = workers.stream()
                        .filter(w -> !w.done)
                        .toList();
                if (alive.isEmpty()) break;
                List<Integer> enabled = alive.stream()
                        .filter(w -> w.ready.getAsBoolean())
                        .map(w -> w.id)
                        .toList();
                if (enabled.isEmpty()) {
                    deadlock = locks.stream()
                            .filter(lock -> lock.owner >= 0)
                            .map(lock -> lock.name + ":" + (char) ('A' + lock.owner))
                            .collect(Collectors.joining(", ", "held={", "}"));
                    break;
                }
                int next = strategy.pick(enabled, last);
                if (!enabled.contains(next)) {
                    throw new IllegalArgumentException("Disabled thread: " + next);
                }
                trace.append((char) ('A' + next));
                last = next;
                workers.get(next).turn.release();
                yielded.acquireUninterruptibly();
            }
        } finally {
            aborted = true;
            for (Worker w : workers) w.turn.release();
            boolean interrupted = false;
            for (Worker w : workers) {
                while (w.thread.isAlive()) {
                    try {
                        w.thread.join();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Трасса прогона: буквы потоков, выбранных во всех решениях подряд, например "AAAABB".
     */
    public String traceString() {
        return trace.toString();
    }

    /**
     * window() из тестовых программ: пауза перед следующей операцией с общими данными обязательна.
     */
    public static void point() {
        current().touched = true;
        record("window");
    }

    /**
     * Ждать, пока cond не станет true. До этого поток не попадает в enabled.
     */
    public static void await(BooleanSupplier cond) {
        operation(cond);
        record("await done");
    }
}
