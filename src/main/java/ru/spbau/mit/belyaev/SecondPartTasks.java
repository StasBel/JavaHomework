package ru.spbau.mit.belyaev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public final class SecondPartTasks {

    private SecondPartTasks() {
    }

    // Найти строки из переданных файлов, в которых встречается указанная подстрока.
    public static List<String> findQuotes(List<String> paths, CharSequence sequence) {
        return paths
                .stream()
                .map(Paths::get)
                .flatMap(path -> {
                    try {
                        return Files.lines(path);
                    } catch (IOException e) {
                        return Stream.empty();
                    }})
                .filter(string -> string.contains(sequence))
                .collect(Collectors.toList());
    }

    // В квадрат с длиной стороны 1 вписана мишень.
    // Стрелок атакует мишень и каждый раз попадает в произвольную точку квадрата.
    // Надо промоделировать этот процесс с помощью класса java.util.Random и посчитать, какова вероятность попасть в мишень.
    private static final Random RANDOM = new Random();
    private static final Integer ROUNDS = 100000000;
    private static final double RADIUS = 0.5d;

    private static double sqr(double value) {
        return Math.pow(value, 2);
    }

    private static double calcDist(double x, double y) {
        return Math.sqrt(sqr(x - RADIUS) + sqr(y - RADIUS));
    }

    public static double piDividedBy4() {
        return DoubleStream
                .generate(() -> calcDist(RANDOM.nextDouble(), RANDOM.nextDouble()))
                .limit(ROUNDS)
                .map(dist -> dist <= RADIUS ? 1 : 0)
                .average()
                .getAsDouble();
    }

    // Дано отображение из имени автора в список с содержанием его произведений.
    // Надо вычислить, чья общая длина произведений наибольшая.
    public static String findPrinter(Map<String, List<String>> compositions) {
        return compositions
                .entrySet()
                .stream()
                .max(Comparator.comparingInt(entry -> entry.getValue()
                        .stream()
                        .mapToInt(String::length)
                        .sum()))
                .orElse(new AbstractMap.SimpleEntry<>(null, null))
                .getKey();
    }

    // Вы крупный поставщик продуктов. Каждая торговая сеть делает вам заказ в виде Map<Товар, Количество>.
    // Необходимо вычислить, какой товар и в каком количестве надо поставить.
    public static Map<String, Integer> calculateGlobalOrder(List<Map<String, Integer>> orders) {
        return orders
                .stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.summingInt(Map.Entry::getValue)));
    }
}
