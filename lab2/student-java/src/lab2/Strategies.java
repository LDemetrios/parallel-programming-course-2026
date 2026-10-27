package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/* Стратегии выбора следующего потока.
 * FirstAlive — этап 1, Replay — этап 2, RandomPick — этап 3, Dfs — этап 4 (ограничение bound — этап 5).
 * enabled — номера потоков, которые могут сделать шаг, по возрастанию; last — прошлый выбранный (-1 в начале). */
public final class Strategies {
    private Strategies() {
    }

    /**
     * Всегда первый поток из enabled.
     */
    public static final class FirstAlive implements Strategy {
        public int pick(List<Integer> enabled, int last) {
            return enabled.get(0);
        }
    }

    /**
     * Проигрывает трассу: на i-м решении выбирает поток trace[i] ('A' -> 0, 'B' -> 1).
     */
    public static final class Replay implements Strategy {
        private final String trace;
        private int position;

        public Replay(String trace) {
            this.trace = trace;
        }

        /**
         * Сбросить позицию в трассе перед прогоном.
         */
        public void beginRun() {
            position = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            if (position == trace.length()) {
                throw new IllegalArgumentException("Replay trace exhausted");
            }
            int id = trace.charAt(position++) - 'A';
            if (!enabled.contains(id)) {
                throw new IllegalArgumentException("Replay thread unavailable at step " + position);
            }
            return id;
        }
    }

    /**
     * Случайный поток: enabled.get(rnd.nextInt(enabled.size())), rnd = new java.util.Random(seed).
     */
    public record RandomPick(Random rnd) implements Strategy {
        public RandomPick(long seed) {
            this(new Random(seed));
        }

        public int pick(List<Integer> enabled, int last) {
            return enabled.get(rnd.nextInt(enabled.size()));
        }
    }

    /**
     * Stateless DFS по дереву выборов: каждый прогон с нуля, повторяет префикс прошлого и берёт следующий вариант.
     * bound — максимум вытеснений (этап 5); для dfs runner передаёт Integer.MAX_VALUE.
     */
    public static final class Dfs implements Strategy {
        private final int bound;
        private final List<List<Integer>> options = new ArrayList<>();
        private final List<Integer> choices = new ArrayList<>();
        private int position, preemptions;

        public Dfs(int bound) {
            if (bound < 0) {
                throw new IllegalArgumentException("Negative preemption bound");
            }
            this.bound = bound;
        }

        /**
         * Подготовиться к новому прогону (позиция в пути, счётчик вытеснений).
         */
        public void beginRun() {
            position = 0;
            preemptions = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            boolean canContinue = enabled.contains(last);
            if (position == choices.size()) {
                List<Integer> candidates = enabled.stream()
                        .filter(id -> !canContinue || id == last || preemptions < bound)
                        .toList();
                options.add(candidates);
                choices.add(0);
            }
            int id = options.get(position).get(choices.get(position));
            if (canContinue && id != last) {
                preemptions++;
            }
            position++;
            return id;
        }

        /**
         * Выбрать следующий путь; false — дерево обойдено.
         */
        public boolean endRun() {
            while (!choices.isEmpty()) {
                int i = choices.size() - 1;
                int next = choices.get(i) + 1;
                if (next < options.get(i).size()) {
                    choices.set(i, next);
                    return true;
                }
                choices.remove(i);
                options.remove(i);
            }
            return false;
        }
    }
}
