package lab2;

/* Этап 2: лок, о котором знает планировщик. Поток, ждущий занятый лок, не попадает в enabled;
 * обычный synchronized / ReentrantLock здесь нельзя — поток уснёт с ходом в руках и всё зависнет.
 * Лог: "lock <имя>", "unlock <имя>". Планировщик должен знать все локи, чтобы печатать held={a:A, b:B}. */
public final class SchedLock {
    final String name;
    private final Scheduler scheduler;
    int owner = -1;

    /** Создаётся в setup() тестовой программы; зарегистрируйте лок в планировщике s. */
    public SchedLock(Scheduler s, String name) {
        this.name = name;
        scheduler = s;
        s.locks.add(this);
    }

    private int caller() {
        Scheduler.Worker w = Scheduler.current();
        if (w.scheduler != scheduler) throw new IllegalStateException("Lock belongs to another scheduler");
        return w.id;
    }

    /** Пауза; если лок занят — ждать, не участвуя в выборе; затем стать владельцем. */
    public void lock() {
        int id = caller();
        Scheduler.operation(() -> owner == -1);
        owner = id;
        Scheduler.record("lock " + name);
    }

    /** Пауза; освободить лок, ждавшие потоки снова могут быть выбраны. */
    public void unlock() {
        int id = caller();
        if (owner != id) throw new IllegalMonitorStateException("Not owner of " + name);
        Scheduler.operation();
        owner = -1;
        Scheduler.record("unlock " + name);
    }
}
