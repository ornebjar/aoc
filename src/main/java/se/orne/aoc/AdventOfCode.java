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

public abstract class AdventOfCode<T> {

    private boolean isProgressTracking = false;
    private String currentProgress = "";
    private String currentHeader;

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

    abstract public T input(String input);

    public Object part1(T input) {
        return null;
    }

    public Object part2(T input) {
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
