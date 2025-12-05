package se.phet.aoc;

enum Part {
    ONE, TWO;

    char subscript() {
        return switch (this) {
            case ONE -> '₁';
            case TWO -> '₂';
        };
    }
}