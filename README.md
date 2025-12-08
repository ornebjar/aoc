# Advent of Code

[![Maven Central](https://img.shields.io/maven-central/v/se.orne.aoc/aoc.svg?label=Maven%20Central)](https://search.maven.org/artifact/se.orne.aoc/aoc)

This is a base library for Advent of Code solutions. It is intended to be used as a starting point for solving the puzzles.

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>se.orne.aoc</groupId>
    <artifactId>aoc</artifactId>
    <version>2.0.8</version>
</dependency>
```

## Usage

To use this library, create a new class that extends the `AdventOfCode` class. Implement the `input`, `part1`, and `part2` methods. The `input` method should process the input and return the data that will be used in the `part1` and `part2` methods. The `part1` and `part2` methods should implement the solutions to the puzzles.

### Structure

The class that extends `AdventOfCode` should be named `SomethingX`, where `X` is the day of the puzzle. The class should be placed in the `something.somethingYYYY` package, where `YYYY` is the year of the puzzle.

### Example

```java
package something.yearYYYY;

import se.orne.aoc.AdventOfCode;

public class DayX extends AdventOfCode<T> {

    @Override
    public T input(String input) {
        // Process input
        return input;
    }

    @Override
    public Object part1(T input) {
        // Implement part 1 solution
        return null;
    }

    @Override
    public Object part2(T input) {
        // Implement part 2 solution
        return null;
    }
}
```

### Cookie

Your advent of code session cookie is required to download the input data. The cookie should be stored in a file named `.cookie` in the data directory. The cookie file should contain only the session cookie value.

### Data

The input data for each day will be automatically downloaded and stored in the `data/YYYY/` directory, when the solution is run. The data will only be downloaded if it does not already exist in the directory.