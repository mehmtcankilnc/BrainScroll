import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline maker of the 16x16 arrow-crossword boards (docs/yeni-oyunlar-plan.md). Run it from the project root:
 *
 *   java tools/crossword/Gen.java make TR 100 600       (language, how many boards, seconds at most)
 *   java tools/crossword/Gen.java check                 (validates the boards of tools/crossword/out against the bank)
 *   java tools/crossword/Gen.java write                 (writes CrosswordBoards.kt from tools/crossword/out)
 *
 * A board is 256 characters: '#' for a clue square, the capital letter for every other square. The rules a board follows (the app
 * checks the same ones in its tests):
 *  - every run of two or more letters, across or down, is a word of the bank, and no word is on the board twice;
 *  - a run has a clue square next to its first letter: the one before it in its direction (an arrow that goes straight), or the
 *    one above it (across) or on its left (down), an arrow that turns once;
 *  - every letter square is in at least one run; every clue square holds at least one clue and at most two;
 *  - no block of 2x2 letters, no block of 2x2 clue squares.
 *
 * The way it works: a layout (which squares are clues) is drawn at random and mended by simulated annealing until it follows the
 * rules; the runs of the layout are then filled with words by a search that keeps the crossings right. A layout the bank cannot
 * fill is left and another is drawn.
 */
public class Gen {
    static final int N = 16;
    static final int CELLS = N * N;
    static final Path ROOT = Paths.get("").toAbsolutePath();
    static final Path CROSSWORD = ROOT.resolve("shared/src/commonMain/kotlin/com/mehmtcan/brainscroll/game/crossword");
    static final Path OUT = ROOT.resolve("tools/crossword/out");

    static int MAX_RUN = 8;
    static int MIN_CLUES = Integer.getInteger("minclues", 66);
    static int MAX_CLUES = Integer.getInteger("maxclues", 84);

    // ------------------------------------------------------------------ the bank

    static Map<String, String> loadBank(String language) throws IOException {
        Map<String, String> entries = new HashMap<>();
        Path dir = ROOT.resolve("tools/crossword/bank");
        try (var files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.getFileName().toString().startsWith(language.toLowerCase() + "_")).sorted()::iterator) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    line = line.strip();
                    if (line.isEmpty()) continue;
                    int bar = line.indexOf('|');
                    entries.put(line.substring(0, bar).strip(), line.substring(bar + 1).strip());
                }
            }
        }
        return entries;
    }

    static class Words {
        final String[][] byLength = new String[MAX_LEN + 1][];
        final Map<Character, Integer> letterIndex = new HashMap<>();
        final BitSet[][][] letterSets = new BitSet[MAX_LEN + 1][][];
        final Set<String> all = new HashSet<>();
        static final int MAX_LEN = 12;

        Words(Set<String> words) {
            all.addAll(words);
            Set<Character> letters = new java.util.TreeSet<>();
            for (String w : words) for (char ch : w.toCharArray()) letters.add(ch);
            for (char ch : letters) letterIndex.put(ch, letterIndex.size());
            for (int len = 2; len <= MAX_LEN; len++) {
                final int length = len;
                String[] list = words.stream().filter(w -> w.length() == length).sorted().toArray(String[]::new);
                byLength[len] = list;
                letterSets[len] = new BitSet[len][letterIndex.size()];
                for (int pos = 0; pos < len; pos++) {
                    for (int l = 0; l < letterIndex.size(); l++) letterSets[len][pos][l] = new BitSet(list.length);
                    for (int i = 0; i < list.length; i++) letterSets[len][pos][letterIndex.get(list[i].charAt(pos))].set(i);
                }
            }
        }

        int size(int length) {
            return length <= MAX_LEN && byLength[length] != null ? byLength[length].length : 0;
        }
    }

    // ------------------------------------------------------------------ runs and owners

    /** The runs of a layout: for run k, whether it goes across, its first square and its length. */
    static class Runs {
        final int[] start = new int[300];
        final boolean[] across = new boolean[300];
        final int[] length = new int[300];
        int count;

        void compute(boolean[] clue) {
            count = 0;
            for (int r = 0; r < N; r++) {
                int c = 0;
                while (c < N) {
                    if (clue[r * N + c]) {
                        c++;
                        continue;
                    }
                    int s = c;
                    while (c < N && !clue[r * N + c]) c++;
                    if (c - s >= 2) add(r * N + s, true, c - s);
                }
            }
            for (int c = 0; c < N; c++) {
                int r = 0;
                while (r < N) {
                    if (clue[r * N + c]) {
                        r++;
                        continue;
                    }
                    int s = r;
                    while (r < N && !clue[r * N + c]) r++;
                    if (r - s >= 2) add(s * N + c, false, r - s);
                }
            }
        }

        private void add(int s, boolean a, int len) {
            start[count] = s;
            across[count] = a;
            length[count] = len;
            count++;
        }

        int cell(int k, int pos) {
            return across[k] ? start[k] + pos : start[k] + pos * N;
        }

        /** The clue squares that may hold the clue of run k: the one before it, and the one that turns once (or -1). */
        int straight(int k, boolean[] clue) {
            int r = start[k] / N, c = start[k] % N;
            int rr = across[k] ? r : r - 1, cc = across[k] ? c - 1 : c;
            return rr >= 0 && cc >= 0 && clue[rr * N + cc] ? rr * N + cc : -1;
        }

        int bent(int k, boolean[] clue) {
            int r = start[k] / N, c = start[k] % N;
            int rr = across[k] ? r - 1 : r, cc = across[k] ? c : c - 1;
            return rr >= 0 && cc >= 0 && clue[rr * N + cc] ? rr * N + cc : -1;
        }
    }

    /**
     * Gives every run a clue square, a clue square at most two runs and at least one. Returns how many things could not be done (0 = it
     * works): clue squares without a run, runs without a clue square. owner[k] is the clue square of run k when it works.
     */
    static int owners(boolean[] clue, Runs runs, int[] owner) {
        int n = runs.count;
        int[][] options = new int[n][];
        int missing = 0;
        List<Integer> clueCells = new ArrayList<>();
        int[] clueIndex = new int[CELLS];
        Arrays.fill(clueIndex, -1);
        for (int i = 0; i < CELLS; i++) {
            if (clue[i]) {
                clueIndex[i] = clueCells.size();
                clueCells.add(i);
            }
        }
        int m = clueCells.size();
        List<Integer>[] runsOf = new List[m];
        for (int i = 0; i < m; i++) runsOf[i] = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            int a = runs.straight(k, clue), b = runs.bent(k, clue);
            if (a >= 0 && b >= 0) options[k] = new int[]{clueIndex[a], clueIndex[b]};
            else if (a >= 0) options[k] = new int[]{clueIndex[a]};
            else if (b >= 0) options[k] = new int[]{clueIndex[b]};
            else {
                options[k] = new int[0];
                missing++;
            }
            for (int o : options[k]) runsOf[o].add(k);
        }
        // Every clue square gets a run of its own (a matching).
        int[] matchRun = new int[n];
        Arrays.fill(matchRun, -1);
        int unmatched = 0;
        for (int i = 0; i < m; i++) {
            if (!augment(i, runsOf, matchRun, new boolean[n])) unmatched++;
        }
        // The other runs are placed by augmenting paths, which move runs between clue squares but never leave one empty.
        int[] load = new int[m];
        List<Integer>[] held = new List[m];
        for (int i = 0; i < m; i++) held[i] = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            if (matchRun[k] >= 0) {
                load[matchRun[k]]++;
                held[matchRun[k]].add(k);
            }
        }
        int placed = 0, unplaced = 0;
        int[] at = new int[n];
        Arrays.fill(at, -1);
        for (int k = 0; k < n; k++) if (matchRun[k] >= 0) at[k] = matchRun[k];
        for (int k = 0; k < n; k++) {
            if (matchRun[k] >= 0 || options[k].length == 0) continue;
            if (place(k, options, held, at, new boolean[m], -1)) placed++;
            else unplaced++;
        }
        if (owner != null && unmatched == 0 && unplaced == 0 && missing == 0) {
            for (int k = 0; k < n; k++) owner[k] = clueCells.get(at[k]);
        }
        return unmatched + unplaced + missing;
    }

    private static boolean augment(int clueIdx, List<Integer>[] runsOf, int[] matchRun, boolean[] seen) {
        for (int k : runsOf[clueIdx]) {
            if (seen[k]) continue;
            seen[k] = true;
            if (matchRun[k] < 0 || augment(matchRun[k], runsOf, matchRun, seen)) {
                matchRun[k] = clueIdx;
                return true;
            }
        }
        return false;
    }

    private static boolean place(int k, int[][] options, List<Integer>[] held, int[] at, boolean[] seen, int leaving) {
        for (int cell : options[k]) {
            if (cell == leaving || seen[cell]) continue;
            seen[cell] = true;
            if (held[cell].size() < 2) {
                held[cell].add(k);
                at[k] = cell;
                return true;
            }
            for (int j : new ArrayList<>(held[cell])) {
                if (place(j, options, held, at, seen, cell)) {
                    held[cell].remove((Integer) j);
                    held[cell].add(k);
                    at[k] = cell;
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ layouts

    /** Blocks of 2x2 clue squares (never allowed) and of 2x2 letters (a few are allowed); the result is how many are too many. */
    static int blocks(boolean[] clue) {
        int clueBlocks = 0, letterBlocks = 0;
        for (int r = 0; r < N - 1; r++) {
            for (int c = 0; c < N - 1; c++) {
                int n = (clue[r * N + c] ? 1 : 0) + (clue[r * N + c + 1] ? 1 : 0) + (clue[(r + 1) * N + c] ? 1 : 0) + (clue[(r + 1) * N + c + 1] ? 1 : 0);
                if (n == 4) clueBlocks++;
                if (n == 0) letterBlocks++;
            }
        }
        return clueBlocks + Math.max(0, letterBlocks - MAX_LETTER_BLOCKS);
    }

    static int MAX_LETTER_BLOCKS = Integer.getInteger("letterblocks", 16);

    /** How far a layout is from usable: 0 means it follows every rule. */
    static int faults(boolean[] clue, Runs runs, int[] caps) {
        runs.compute(clue);
        boolean[] covered = new boolean[CELLS];
        int faults = 0;
        int[] counts = new int[Words.MAX_LEN + 1];
        for (int k = 0; k < runs.count; k++) {
            int len = runs.length[k];
            if (len > MAX_RUN) faults += len - MAX_RUN;
            else counts[len]++;
            for (int p = 0; p < len; p++) covered[runs.cell(k, p)] = true;
        }
        int clues = 0;
        for (int i = 0; i < CELLS; i++) {
            if (clue[i]) clues++;
            else if (!covered[i]) faults++;
        }
        if (clues < MIN_CLUES) faults += MIN_CLUES - clues;
        if (clues > MAX_CLUES) faults += clues - MAX_CLUES;
        faults += blocks(clue);
        for (int len = 2; len <= MAX_RUN; len++) if (counts[len] > caps[len]) faults += counts[len] - caps[len];
        faults += owners(clue, runs, null);
        return faults;
    }

    static boolean[] makeLayout(Random rng, int[] caps, int steps) {
        Runs runs = new Runs();
        boolean[] clue = new boolean[CELLS];
        for (int i = 0; i < CELLS; i++) clue[i] = rng.nextDouble() < 0.3;
        int score = faults(clue, runs, caps);
        score = anneal(rng, clue, runs, caps, score, steps, 1.2, 0.12);
        boolean[] best = clue.clone();
        int bestScore = score;
        // Kicks: when it is nearly right, a few squares are turned over and the layout is mended again.
        for (int round = 0; round < 60 && bestScore > 0; round++) {
            boolean[] trial = best.clone();
            int kicks = 2 + rng.nextInt(5);
            for (int k = 0; k < kicks; k++) {
                int i = rng.nextInt(CELLS);
                trial[i] = !trial[i];
            }
            int trialScore = faults(trial, runs, caps);
            trialScore = anneal(rng, trial, runs, caps, trialScore, steps / 8, 0.35, 0.1);
            if (trialScore <= bestScore) {
                best = trial;
                bestScore = trialScore;
            }
        }
        lastScore = bestScore;
        lastLayout = best.clone();
        return bestScore == 0 ? best : null;
    }

    static int anneal(Random rng, boolean[] clue, Runs runs, int[] caps, int score, int steps, double t0, double t1) {
        for (int s = 0; s < steps && score > 0; s++) {
            double t = t0 * Math.pow(t1 / t0, (double) s / steps);
            int i = rng.nextInt(CELLS);
            int j = -1;
            if (rng.nextDouble() < 0.3) {
                int r = i / N, c = i % N;
                int[][] d = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
                int[] move = d[rng.nextInt(4)];
                int rr = r + move[0], cc = c + move[1];
                if (rr >= 0 && rr < N && cc >= 0 && cc < N) j = rr * N + cc;
            }
            clue[i] = !clue[i];
            if (j >= 0) clue[j] = !clue[j];
            int next = faults(clue, runs, caps);
            if (next <= score || rng.nextDouble() < Math.exp((score - next) / t)) {
                score = next;
            } else {
                clue[i] = !clue[i];
                if (j >= 0) clue[j] = !clue[j];
            }
        }
        return score;
    }

    /** Tries every single change and then every pair of changes of a layout that is nearly right. */
    static int polish(boolean[] clue, Runs runs, int[] caps, int score) {
        for (int i = 0; i < CELLS; i++) {
            clue[i] = !clue[i];
            int f = faults(clue, runs, caps);
            if (f == 0) return 0;
            clue[i] = !clue[i];
        }
        for (int i = 0; i < CELLS; i++) {
            clue[i] = !clue[i];
            for (int j = i + 1; j < CELLS; j++) {
                clue[j] = !clue[j];
                int f = faults(clue, runs, caps);
                if (f == 0) return 0;
                clue[j] = !clue[j];
            }
            clue[i] = !clue[i];
        }
        return score;
    }

    static int lastScore;
    static boolean[] lastLayout;

    // ------------------------------------------------------------------ the fill

    static class Fill {
        final Words words;
        final Runs runs;
        final int n;
        final int[][] cells;
        final int[][][] links; // links[i] = list of {position in i, other run, position in other}
        final Random rng;
        int nodes;
        final int budget;
        final int[] chosen;

        Fill(Words words, Runs runs, Random rng, int budget) {
            this.words = words;
            this.runs = runs;
            this.rng = rng;
            this.budget = budget;
            n = runs.count;
            cells = new int[n][];
            int[] first = new int[CELLS];
            int[] firstPos = new int[CELLS];
            Arrays.fill(first, -1);
            List<List<int[]>> linkLists = new ArrayList<>();
            for (int i = 0; i < n; i++) linkLists.add(new ArrayList<>());
            for (int i = 0; i < n; i++) {
                cells[i] = new int[runs.length[i]];
                for (int p = 0; p < cells[i].length; p++) {
                    int cell = runs.cell(i, p);
                    cells[i][p] = cell;
                    if (first[cell] < 0) {
                        first[cell] = i;
                        firstPos[cell] = p;
                    } else {
                        int j = first[cell], q = firstPos[cell];
                        linkLists.get(i).add(new int[]{p, j, q});
                        linkLists.get(j).add(new int[]{q, i, p});
                    }
                }
            }
            links = new int[n][][];
            for (int i = 0; i < n; i++) links[i] = linkLists.get(i).toArray(new int[0][]);
            chosen = new int[n];
        }

        boolean propagate(BitSet[] doms, ArrayList<Integer> queue) {
            while (!queue.isEmpty()) {
                int i = queue.remove(queue.size() - 1);
                int li = cells[i].length;
                for (int[] link : links[i]) {
                    int a = link[0], j = link[1], b = link[2];
                    long mask = 0;
                    BitSet[] sets = words.letterSets[li][a];
                    for (int l = 0; l < sets.length; l++) {
                        if (doms[i].intersects(sets[l])) mask |= 1L << l;
                    }
                    int lj = cells[j].length;
                    BitSet allowed = new BitSet();
                    BitSet[] other = words.letterSets[lj][b];
                    for (int l = 0; l < other.length; l++) if ((mask >> l & 1) != 0) allowed.or(other[l]);
                    int before = doms[j].cardinality();
                    doms[j].and(allowed);
                    int after = doms[j].cardinality();
                    if (after == 0) return false;
                    if (after < before) queue.add(j);
                }
            }
            return true;
        }

        String[] solve() {
            BitSet[] doms = new BitSet[n];
            for (int i = 0; i < n; i++) {
                int size = words.size(cells[i].length);
                if (size == 0) return null;
                doms[i] = new BitSet(size);
                doms[i].set(0, size);
            }
            ArrayList<Integer> queue = new ArrayList<>();
            for (int i = 0; i < n; i++) queue.add(i);
            if (!propagate(doms, queue)) return null;
            Arrays.fill(chosen, -1);
            if (!search(doms)) return null;
            String[] result = new String[n];
            for (int i = 0; i < n; i++) result[i] = words.byLength[cells[i].length][chosen[i]];
            return result;
        }

        boolean search(BitSet[] doms) {
            if (++nodes > budget) return false;
            int best = -1, bestSize = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (chosen[i] >= 0) continue;
                int size = doms[i].cardinality();
                if (size < bestSize) {
                    bestSize = size;
                    best = i;
                }
            }
            if (best < 0) return true;
            int len = cells[best].length;
            int[] candidates = doms[best].stream().toArray();
            for (int x = candidates.length - 1; x > 0; x--) {
                int y = rng.nextInt(x + 1);
                int tmp = candidates[x];
                candidates[x] = candidates[y];
                candidates[y] = tmp;
            }
            for (int word : candidates) {
                BitSet[] next = new BitSet[n];
                for (int i = 0; i < n; i++) next[i] = (BitSet) doms[i].clone();
                next[best].clear();
                next[best].set(word);
                ArrayList<Integer> queue = new ArrayList<>();
                queue.add(best);
                boolean ok = true;
                for (int j = 0; j < n && ok; j++) {
                    if (j != best && chosen[j] < 0 && cells[j].length == len && next[j].get(word)) {
                        next[j].clear(word);
                        if (next[j].isEmpty()) ok = false;
                        else queue.add(j);
                    }
                }
                if (ok && propagate(next, queue)) {
                    chosen[best] = word;
                    if (search(next)) return true;
                    chosen[best] = -1;
                }
                if (nodes > budget) return false;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ checking a board

    /** null if the board follows every rule, else why not. */
    static String validate(String board, Words words) {
        if (board.length() != CELLS) return "length";
        boolean[] clue = new boolean[CELLS];
        for (int i = 0; i < CELLS; i++) clue[i] = board.charAt(i) == '#';
        Runs runs = new Runs();
        runs.compute(clue);
        Set<String> seen = new HashSet<>();
        boolean[] covered = new boolean[CELLS];
        for (int k = 0; k < runs.count; k++) {
            StringBuilder sb = new StringBuilder();
            for (int p = 0; p < runs.length[k]; p++) {
                sb.append(board.charAt(runs.cell(k, p)));
                covered[runs.cell(k, p)] = true;
            }
            String w = sb.toString();
            if (!words.all.contains(w)) return "not a word: " + w;
            if (!seen.add(w)) return "twice: " + w;
        }
        for (int i = 0; i < CELLS; i++) if (!clue[i] && !covered[i]) return "a letter in no run";
        if (blocks(clue) != 0) return "a block";
        if (owners(clue, runs, null) != 0) return "no owners";
        return null;
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        switch (args[0]) {
            case "make" -> make(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            case "check" -> check();
            case "layouts" -> layoutsTest(Integer.parseInt(args[1]));
            case "proxy" -> proxyTest(Integer.parseInt(args[1]), Double.parseDouble(args[2]));
            case "write" -> write();
            case "bank" -> bank();
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    static void make(String language, int count, int seconds) throws Exception {
        Words words = new Words(loadBank(language).keySet());
        int[] caps = new int[Words.MAX_LEN + 1];
        StringBuilder sizes = new StringBuilder();
        for (int len = 2; len <= MAX_RUN; len++) {
            caps[len] = Math.max(3, (int) (words.size(len) * 0.3));
            if (Integer.getInteger("cap" + len) != null) caps[len] = Integer.getInteger("cap" + len);
            sizes.append(len).append(':').append(words.size(len)).append(' ');
        }
        System.out.println(language + " words " + words.all.size() + "  by length " + sizes);
        Path file = OUT.resolve(language + ".txt");
        Set<String> boards = new HashSet<>();
        if (Files.exists(file)) boards.addAll(Files.readAllLines(file));
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        AtomicInteger layouts = new AtomicInteger(), filled = new AtomicInteger();
        List<Thread> pool = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int seed = t * 7919 + (int) (System.nanoTime() % 100000);
            Thread thread = new Thread(() -> {
                Random rng = new Random(seed);
                while (System.currentTimeMillis() < deadline) {
                    synchronized (boards) {
                        if (boards.size() >= count) return;
                    }
                    boolean[] clue = makeLayout(rng, caps, 60000);
                    if (clue == null) continue;
                    layouts.incrementAndGet();
                    Runs runs = new Runs();
                    runs.compute(clue);
                    for (int attempt = 0; attempt < 6; attempt++) {
                        Fill fill = new Fill(words, runs, rng, 4000);
                        String[] result = fill.solve();
                        if (result == null) continue;
                        char[] board = new char[CELLS];
                        for (int i = 0; i < CELLS; i++) board[i] = clue[i] ? '#' : '?';
                        for (int k = 0; k < runs.count; k++) for (int p = 0; p < runs.length[k]; p++) board[runs.cell(k, p)] = result[k].charAt(p);
                        String text = new String(board);
                        String why = validate(text, words);
                        if (why != null) {
                            System.out.println("INVALID " + why);
                            continue;
                        }
                        filled.incrementAndGet();
                        synchronized (boards) {
                            if (boards.add(text)) {
                                try {
                                    Files.write(file, boards, StandardCharsets.UTF_8);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                System.out.println(language + " boards " + boards.size() + "  layouts " + layouts + "  filled " + filled);
                            }
                        }
                    }
                }
            });
            thread.start();
            pool.add(thread);
        }
        for (Thread thread : pool) thread.join();
        System.out.println(language + " done: " + boards.size() + " boards, " + layouts + " layouts, " + filled + " fills");
    }

    /** Tries the search with made-up words (pieces of the 5-letter words of the word puzzle), to see how big a bank has to be. */
    static void proxyTest(int layoutsToTry, double scale) throws Exception {
        List<String> w5 = new ArrayList<>();
        for (String line : Files.readAllLines(ROOT.resolve("shared/src/commonMain/composeResources/files/words/tr_valid.txt"), StandardCharsets.UTF_8)) {
            line = line.strip();
            if (line.length() == 5 && !line.startsWith("#")) w5.add(line.toUpperCase(new java.util.Locale("tr")));
        }
        Random rng = new Random(3);
        int[][] sizes = {{2, 60}, {3, 400}, {4, 800}, {5, 1100}, {6, 600}, {7, 400}, {8, 250}};
        Set<String> words = new HashSet<>();
        for (int[] sz : sizes) {
            int want = (int) (sz[1] * scale);
            int tries = 0;
            Set<String> mine = new HashSet<>();
            while (mine.size() < want && tries++ < 200000) {
                int len = sz[0];
                String a = w5.get(rng.nextInt(w5.size())), b = w5.get(rng.nextInt(w5.size()));
                String w;
                if (len <= 5) w = len == 5 ? a : a.substring(rng.nextInt(5 - len + 1)).substring(0, len);
                else w = (a.substring(rng.nextInt(3)) + b).substring(0, len);
                mine.add(w);
            }
            words.addAll(mine);
        }
        Words dictionary = new Words(words);
        int[] caps = new int[Words.MAX_LEN + 1];
        Arrays.fill(caps, 100);
        caps[2] = Integer.getInteger("cap2", 100);
        caps[3] = Integer.getInteger("cap3", 100);
        long start = System.currentTimeMillis();
        int made = 0, fills = 0, tried = 0;
        for (int i = 0; i < layoutsToTry; i++) {
            boolean[] clue = makeLayout(rng, caps, 80000);
            if (clue == null) continue;
            made++;
            Runs runs = new Runs();
            runs.compute(clue);
            for (int attempt = 0; attempt < 4; attempt++) {
                tried++;
                Fill fill = new Fill(dictionary, runs, rng, Integer.getInteger("budget", 4000));
                String[] result = fill.solve();
                if (result != null) fills++;
            }
        }
        System.out.println("words " + words.size() + " layouts " + made + " fill attempts " + tried + " successes " + fills + " in " + (System.currentTimeMillis() - start) + " ms");
    }

    static void layoutsTest(int count) {
        int[] caps = new int[Words.MAX_LEN + 1];
        Arrays.fill(caps, 100);
        caps[2] = Integer.getInteger("cap2", 100);
        caps[3] = Integer.getInteger("cap3", 100);
        caps[4] = Integer.getInteger("cap4", 100);
        Random rng = new Random(1);
        long start = System.currentTimeMillis();
        int ok = 0;
        for (int i = 0; i < count; i++) {
            boolean[] clue = makeLayout(rng, caps, Integer.parseInt(System.getProperty("steps", "60000")));
            System.out.println("score " + lastScore);
            if (clue == null) {
                if (i == 0) {
                    for (int r = 0; r < N; r++) {
                        StringBuilder sb = new StringBuilder();
                        for (int c = 0; c < N; c++) sb.append(lastLayout[r * N + c] ? '#' : '.');
                        System.out.println(sb);
                    }
                    Runs runs = new Runs();
                    runs.compute(lastLayout);
                    boolean[] cov = new boolean[CELLS];
                    int longRuns = 0;
                    for (int k = 0; k < runs.count; k++) { if (runs.length[k] > MAX_RUN) longRuns++; for (int p = 0; p < runs.length[k]; p++) cov[runs.cell(k, p)] = true; }
                    int unc = 0, cl = 0;
                    for (int q = 0; q < CELLS; q++) { if (lastLayout[q]) cl++; else if (!cov[q]) { unc++; System.out.println("uncovered " + q / N + "," + q % N); } }
                    System.out.println("owners deficiency " + owners(lastLayout, runs, null) + " blocks " + blocks(lastLayout) + " runs " + runs.count + " uncovered " + unc + " clues " + cl + " long " + longRuns);
                    for (int k = 0; k < runs.count; k++) if (runs.straight(k, lastLayout) < 0 && runs.bent(k, lastLayout) < 0) System.out.println("run without owner at " + runs.start[k] / N + "," + runs.start[k] % N + " across " + runs.across[k] + " len " + runs.length[k]);
                    for (int q = 0; q < CELLS; q++) if (lastLayout[q]) {
                        boolean any = false;
                        for (int k = 0; k < runs.count; k++) if (runs.straight(k, lastLayout) == q || runs.bent(k, lastLayout) == q) any = true;
                        if (!any) System.out.println("idle clue " + q / N + "," + q % N);
                    }
                    for (int r = 0; r < N - 1; r++) for (int c = 0; c < N - 1; c++) { int n = (lastLayout[r*N+c]?1:0)+(lastLayout[r*N+c+1]?1:0)+(lastLayout[(r+1)*N+c]?1:0)+(lastLayout[(r+1)*N+c+1]?1:0); if (n==0||n==4) System.out.println("block at " + r + "," + c + " n=" + n); }
                }
                continue;
            }
            ok++;
            if (ok == 1) {
                for (int r = 0; r < N; r++) {
                    StringBuilder sb = new StringBuilder();
                    for (int c = 0; c < N; c++) sb.append(clue[r * N + c] ? '#' : '.');
                    System.out.println(sb);
                }
                Runs runs = new Runs();
                runs.compute(clue);
                int[] lens = new int[10];
                for (int k = 0; k < runs.count; k++) lens[Math.min(9, runs.length[k])]++;
                System.out.println("runs " + runs.count + " lengths " + Arrays.toString(lens));
            }
        }
        System.out.println(ok + "/" + count + " layouts in " + (System.currentTimeMillis() - start) + " ms");
    }

    static void check() throws Exception {
        for (String language : new String[]{"TR", "EN"}) {
            Path file = OUT.resolve(language + ".txt");
            if (!Files.exists(file)) continue;
            Words words = new Words(loadBank(language).keySet());
            int bad = 0, total = 0;
            for (String board : Files.readAllLines(file)) {
                total++;
                String why = validate(board, words);
                if (why != null) {
                    bad++;
                    System.out.println(language + " board " + total + ": " + why);
                }
            }
            System.out.println(language + ": " + total + " boards, " + bad + " bad");
        }
    }

    /** Writes CrosswordBankData.kt: the bank files, cut in pieces that fit the 64 KB limit of a string constant. */
    static void bank() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.mehmtcan.brainscroll.game.crossword\n\n");
        sb.append("/**\n * The words of the crossword and their clues, one `WORD|clue` per line, made from tools/crossword/bank by `Gen.java bank` (do not\n");
        sb.append(" * edit by hand: change the files of that folder and run it again). The pieces are cut to fit the limit of a string constant.\n */\n");
        sb.append("internal object CrosswordBankData {\n");
        for (String language : new String[]{"TR", "EN"}) {
            Map<String, String> entries = new java.util.TreeMap<>(loadBank(language));
            List<String> pieces = new ArrayList<>();
            StringBuilder piece = new StringBuilder();
            for (Map.Entry<String, String> e : entries.entrySet()) {
                String line = e.getKey() + "|" + e.getValue() + "\n";
                if (piece.toString().getBytes(StandardCharsets.UTF_8).length + line.getBytes(StandardCharsets.UTF_8).length > 30000) {
                    pieces.add(piece.toString());
                    piece = new StringBuilder();
                }
                piece.append(line);
            }
            pieces.add(piece.toString());
            sb.append("    val ").append(language).append(": List<String> = listOf(\n");
            for (String text : pieces) sb.append("        \"\"\"\n").append(text).append("\"\"\",\n");
            sb.append("    )\n");
            if (language.equals("TR")) sb.append("\n");
        }
        sb.append("}\n");
        Files.writeString(CROSSWORD.resolve("CrosswordBankData.kt"), sb.toString(), StandardCharsets.UTF_8);
        System.out.println("bank written");
    }

    static final int BOARDS_PER_LANGUAGE = 120;

    static void write() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.mehmtcan.brainscroll.game.crossword\n\nimport com.mehmtcan.brainscroll.game.wordle.Language\n\n");
        sb.append("/**\n * The ready boards of the crossword, made offline by tools/crossword/Gen.java (do not edit by hand). A board is 256 characters, row by\n");
        sb.append(" * row: a `#` for a clue square, the capital letter for every other square. The app picks one by its number, so a saved game only\n");
        sb.append(" * has to remember the number and the language; a board never changes once it is in this list, new ones are added at the end.\n */\n");
        sb.append("internal object CrosswordBoards {\n");
        for (String language : new String[]{"TR", "EN"}) {
            Path file = OUT.resolve(language + ".txt");
            List<String> boards = Files.exists(file) ? Files.readAllLines(file) : List.of();
            sb.append("    val ").append(language).append(": List<String> = listOf(\n");
            int written = 0;
            for (String board : boards) {
                if (board.isBlank() || written++ >= BOARDS_PER_LANGUAGE) continue;
                sb.append("        \"").append(board).append("\",\n");
            }
            sb.append("    )\n");
            sb.append("\n");
        }
        sb.append("    fun of(language: Language): List<String> = when (language) {\n        Language.TR -> TR\n        Language.EN -> EN\n    }\n");
        sb.append("}\n");
        Files.writeString(CROSSWORD.resolve("CrosswordBoards.kt"), sb.toString(), StandardCharsets.UTF_8);
        System.out.println("written");
    }
}
