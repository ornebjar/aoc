package se.orne.aoc;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static se.orne.aoc.AnsiColor.GREY;
import static se.orne.aoc.AnsiColor.RESET;

final class InputProvider {

    private static final String BASE_URL = "https://adventofcode.com/%s/day/%s";
    private static final String INPUT_URL = BASE_URL + "/input";

    static String readRealInput(String year, String day) {
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

    private static String downloadInput(String year, String day) {
        String cookie = getCookie();
        String url = INPUT_URL.formatted(year, day);
        System.err.println("Downloading file " + url);
        try (var client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(new URI(url))
                            .header("cookie", cookie)
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    static String readExampleInput(String year, String day) {
        Path examplePath = Path.of("data/%s/example%s.csv".formatted(year, day));
        try {
            if (!Files.exists(examplePath)) {
                String html = downloadHtml(year, day);
                String example = extractFirstPreCode(html);
                Files.createDirectories(examplePath.getParent());
                Files.createFile(examplePath);
                Files.writeString(examplePath, example, StandardCharsets.UTF_8);
            } else {
                IO.println(GREY + "Using downloaded example file from " + examplePath + RESET);
            }
            return Files.readString(examplePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read or write example input for " + year + "/" + day, e);
        }
    }

    private static String downloadHtml(String year, String day) {
        String url = BASE_URL.formatted(year, day);
        System.err.println("Downloading example page " + url);
        try (var client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(new URI(url)).build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to download example page for " + year + "/" + day, e);
        }
    }

    private static String extractFirstPreCode(String html) {
        String markerStart = "<pre><code>";
        String markerEnd = "</code></pre>";

        int start = html.indexOf(markerStart);
        if (start < 0) {
            throw new IllegalStateException("No <pre><code> example block found in page");
        }
        start += markerStart.length();
        int end = html.indexOf(markerEnd, start);
        if (end < 0) {
            throw new IllegalStateException("Unterminated <pre><code> block in example page");
        }

        return html.substring(start, end);
    }

    private static String getCookie() {
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
}

