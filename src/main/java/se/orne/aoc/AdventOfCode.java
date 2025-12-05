package se.orne.aoc;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

import static se.orne.aoc.AnsiColor.*;

public abstract class AdventOfCode<T> {

    private boolean isProgressTracking = false;
    private String currentProgress = "";
    private String currentHeader = "";

    public AdventOfCode() {
        Package pkg = getClass().getPackage();
        String year = extractLastNumber(pkg.getName());
        String day = extractLastNumber(getClass().getSimpleName());

        execute(Part.ONE, year, day);
        execute(Part.TWO, year, day);
    }

    private void execute(Part part, String year, String day) {
        currentHeader = BLUE + year + RESET + '·' + CYAN + day + RESET + part.subscript();
        T input = trackProgress(input(readInput(year, day)));
        long startTime = System.currentTimeMillis();
        Object result = switch (part) {
            case ONE -> part1(input);
            case TWO -> part2(input);
        };
        long timeTaken = System.currentTimeMillis() - startTime;

        isProgressTracking = false;
        currentProgress = "";

        IO.println('\r' + currentHeader + ' ' + millisToString(timeTaken) + GREEN + " → " + RESET + result);
    }

    public boolean progressTracking() {
        return true;
    }

    @SuppressWarnings("unchecked")
    private T trackProgress(T input) {
        if (!(input instanceof BaseStream)) {
            return input;
        }

        if (!progressTracking()) {
            return input;
        }

        isProgressTracking = true;
        AtomicInteger counter = new AtomicInteger();
        AtomicInteger previous = new AtomicInteger(-1);

        return switch (input) {
            case Stream<?> stream -> {
                var list = stream.toArray();
                yield (T) Arrays.stream(list).peek(_ ->
                        previous.getAndUpdate(prev -> printProgress(counter, prev, list.length)));
            }
            case IntStream intStream -> {
                var array = intStream.toArray();
                yield (T) Arrays.stream(array).peek(_ ->
                        previous.getAndUpdate(prev -> printProgress(counter, prev, array.length)));
            }
            case LongStream longStream -> {
                var array = longStream.toArray();
                yield (T) Arrays.stream(array).peek(_ ->
                        previous.getAndUpdate(prev -> printProgress(counter, prev, array.length)));
            }
            case DoubleStream doubleStream -> {
                var array = doubleStream.toArray();
                yield (T) Arrays.stream(array).peek(_ ->
                        previous.getAndUpdate(prev -> printProgress(counter, prev, array.length)));
            }
            default -> {
                System.err.println("Warning: Progress tracking not implemented for " + input.getClass());
                yield input;
            }
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

    private static final String URL = "https://adventofcode.com/%s/day/%s/input";

    private String readInput(String year, String day) {
        Path inputPath = Path.of("data/%s/input%s.csv".formatted(year, day));
        try {
            if (!Files.exists(inputPath)) {
                String download = downloadInput(year, day);
                if ("Puzzle inputs differ by user.  Please log in to get your puzzle input.".equals(download.trim())) {
                    throw new AuthenticationException("New cookie required, update data/.cookie");
                }
                Files.createDirectories(inputPath.getParent());
                Files.createFile(inputPath);
                Files.writeString(inputPath, download);
            } else {
                IO.println(GREY + "Using downloaded file from " + inputPath + RESET);
            }
            return Files.readString(inputPath);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String downloadInput(String year, String day) {
        String cookie = getCookie();
        String url = URL.formatted(year, day);
        System.err.println("Downloading file " + url);
        try (var client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder()
                            .uri(new URI(url))
                            .header("cookie", cookie)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getCookie() {
        Path cookiePath = Path.of("data/.cookie");
        try {
            if (!Files.exists(cookiePath)) {
                Files.createDirectories(cookiePath.getParent());
                Files.createFile(cookiePath);
                Files.writeString(cookiePath, "Your cookie here");
            }
            return Files.readString(cookiePath);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
