package se.orne.aoc;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

import static se.orne.aoc.AnsiColor.*;
import static se.orne.aoc.Run.REAL;
import static se.orne.aoc.Run.TEST;

/// Base class for all Advent of Code solutions.
///
/// A typical subclass represents a specific year/day and provides implementations
/// for [#input(String)], [#part1(Object)] and [#part2(Object)].
/// The constructor automatically reads example and real input for the derived
/// class and executes both parts on both inputs, printing a formatted summary
/// including timing and optional progress information.
///
/// @param <T> the parsed input type used by the concrete day implementation
public abstract class AdventOfCode<T> {

    private boolean isProgressTracking = false;
    private String currentProgress = "";
    private String currentHeader;

    /// Creates a new Advent of Code runner for the concrete subclass.
    ///
    /// The constructor infers the year and day from the package name and class
    /// name, reads both example and real inputs, and runs [Part#ONE] and
    /// [Part#TWO] for each input. Output is written immediately using the
    /// [IO] helper.
    public AdventOfCode() {
        Package pkg = getClass().getPackage();
        String year = extractLastNumber(pkg.getName());
        String day = extractLastNumber(getClass().getSimpleName());

        String testInput = InputProvider.readExampleInput(year, day);
        String realInput = InputProvider.readRealInput(year, day);

        for (Part part : Part.values()) {
            execute(testInput, TEST, part, year, day);
            execute(realInput, REAL, part, year, day);
        }
    }

    private void execute(String rawInput, Run run, Part part, String year, String day) {
        currentHeader = BLUE + year + RESET + run.superscript() + CYAN + day + RESET + part.subscript();
        T input = trackProgress(input(rawInput));
        long startTime = System.currentTimeMillis();
        Object result = switch (part) {
            case ONE -> part1(input);
            case TWO -> part2(input);
        };
        long timeTaken = System.currentTimeMillis() - startTime;

        isProgressTracking = false;
        currentProgress = "";

        IO.println('\r' + currentHeader + ' ' + millisToString(timeTaken) + GREEN + " → " + run.color() + result);
    }

    /// Indicates whether progress tracking should be enabled for this instance.
    ///
    /// When this method returns `true`, and the parsed input returned from
    /// [#input(String)] is a non-parallel [java.util.stream.BaseStream],
    /// the framework will wrap it so that a textual progress bar is rendered to
    /// the console while the stream is consumed.
    ///
    /// @return `true` if progress tracking should be enabled; `false` otherwise
    public boolean progressTracking() {
        return true;
    }

    private T trackProgress(T input) {
        if (!progressTracking()) {
            return input;
        }
        if (!(input instanceof BaseStream<?, ?> baseStream)) {
            return input;
        }

        isProgressTracking = true;

        if (baseStream.isParallel()) {
            currentProgress = currentHeader + YELLOW + " Progress tracking not supported for parallel streams." + RESET;
            IO.print(currentProgress);
            return input;
        }

        return switch (input) {
            case Stream<?> stream -> tracker(stream.toArray());
            case IntStream intStream -> tracker(intStream.toArray());
            case LongStream longStream -> tracker(longStream.toArray());
            case DoubleStream doubleStream -> tracker(doubleStream.toArray());
            default -> {
                System.err.println("Warning: Progress tracking not implemented for " + input.getClass());
                yield input;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private T tracker(Object test) {
        AtomicInteger counter = new AtomicInteger();
        AtomicInteger previous = new AtomicInteger(-1);
        return (T) switch (test) {
            case Object[] array -> Arrays.stream(array).peek(_ ->
                    previous.getAndUpdate(prev -> printProgress(counter, prev, array.length)));
            case int[] array -> Arrays.stream(array).peek(_ ->
                    previous.getAndUpdate(prev -> printProgress(counter, prev, array.length)));
            case long[] array -> Arrays.stream(array).peek(_ ->
                    previous.getAndUpdate(prev -> printProgress(counter, prev, array.length)));
            case double[] array -> Arrays.stream(array).peek(_ ->
                    previous.getAndUpdate(prev -> printProgress(counter, prev, array.length)));
            default -> throw new RuntimeException("Unexpected array type: " + test.getClass());
        };
    }

    private static final int BASE = 8;
    private static final int BOXES = 13;

    private int printProgress(AtomicInteger counter, int previous, int size) {
        int currentCount = counter.getAndIncrement();
        int percent = BOXES * BASE * currentCount / size;
        if (previous < percent) {
            var progressBar = getProgressBar(percent);
            int percentageValue = 100 * currentCount / size;

            currentProgress = String.format("%s %s%s%s%%%s", currentHeader, progressBar,
                    RESET, percentageValue, RESET);
            IO.print("\r" + currentProgress);
        }
        return percent;
    }

    private static String getProgressBar(int percent) {
        StringBuilder sb = new StringBuilder(GREEN);
        for (int i = 0; i < BOXES; i++) {
            int diff = percent - i * BASE;
            if (diff >= BASE) {
                // 0: 0x2588 = █
                sb.append((char) 0x2588);
            } else if (diff > 0) {
                // 7: 0x2589 = ▉
                // 6: 0x258A = ▊
                // 5: 0x258B = ▋
                // 4: 0x258C = ▌
                // 3: 0x258D = ▍
                // 2: 0x258E = ▎
                // 1: 0x258F = ▏
                sb.append((char) (0x2590 - diff));
            } else {
                sb.append(' ');
            }
        }
        sb.append('▏');
        return sb.toString();
    }

    /// Logs a message to the console in a way that cooperates with the progress bar.
    ///
    /// If progress tracking is currently active, the message is printed on a new
    /// line and the progress bar is immediately re-rendered underneath. If no
    /// progress tracking is active, the message is printed as a normal line.
    ///
    /// @param message the message to log
    @SuppressWarnings("unused")
    protected void log(String message) {
        if (isProgressTracking) {
            IO.print("\r" + message + "\n" + currentProgress);
        } else {
            IO.println(message);
        }
    }

    private static String extractLastNumber(String string) {
        return string.replaceAll("^.*?(\\d+)$", "$1");
    }

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
            "0.00",
            new DecimalFormatSymbols(Locale.US)
    );

    private static String millisToString(long ms) {
        if (ms >= 1000) {
            String value = DECIMAL_FORMAT.format((double) ms / 1000);
            return "%s%s%ss".formatted(RED, value, RESET);
        } else {
            return "%s%s%sms".formatted(YELLOW, ms, RESET);
        }
    }

    /// Parses the raw puzzle input into the type `T` used by this solution.
    ///
    /// Implementations are free to choose any representation that is convenient
    /// for the puzzle (for example, a list of lines, a custom record type, or a
    /// precomputed data structure). The returned value is passed to
    /// [#part1(Object)] and [#part2(Object)].
    ///
    /// @param input the raw puzzle input as read from the input file
    /// @return a parsed representation of the input
    public abstract T input(String input);

    /// Solves part 1 of the puzzle for the given parsed input.
    ///
    /// @param input the parsed puzzle input produced by [#input(String)]
    /// @return the answer for part 1 (any type with a meaningful `toString()`)
    public Object part1(@SuppressWarnings("unused") T input) {
        return null;
    }

    /// Solves part 2 of the puzzle for the given parsed input.
    ///
    /// @param input the parsed puzzle input produced by [#input(String)]
    /// @return the answer for part 2 (any type with a meaningful `toString()`)
    public Object part2(@SuppressWarnings("unused") T input) {
        return null;
    }

    static void main() throws Throwable {
        try {
            String command = System.getProperty("sun.java.command");
            String[] commandArgs = command.split(" ");
            String className = commandArgs[0];
            var dayClass = Class.forName(className);
            dayClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException |
                 IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
