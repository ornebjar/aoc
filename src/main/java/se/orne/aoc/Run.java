package se.orne.aoc;

enum Run {
    TEST, REAL;

    String superscript() {
        return switch (this) {
            case TEST -> "ᵉˣ";
            case REAL -> "·";
        };
    }

    String color() {
        return switch (this) {
            case TEST -> AnsiColor.GREY;
            case REAL -> AnsiColor.RESET;
        };
    }
}