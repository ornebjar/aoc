package se.orne.aoc;

class AnsiColor {
    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\u001B[34m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String GREY = "\u001B[90m";
    public static final String RED = "\u001B[31m";

    private AnsiColor() {
        // Utility class, prevent instantiation
    }
}

