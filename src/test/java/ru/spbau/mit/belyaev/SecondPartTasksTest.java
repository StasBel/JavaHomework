package ru.spbau.mit.belyaev;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SecondPartTasksTest {
    @Test
    public void testFindQuotes() {
        final String FILE1 = "src/test/java/ru/spbau/mit/belyaev/File1.txt";
        final String FILE2 = "src/test/java/ru/spbau/mit/belyaev/File2.txt";
        final String FILE3 = "src/test/java/ru/spbau/mit/belyaev/File3.txt";
        final List<String> paths = Arrays.asList(FILE1, FILE2, FILE3);

        final List<String> answer = Arrays.asList(
                "Without EETS editions, study of medieval English texts would hardly",
                "As its name states, EETS was begun as a 'club', and it retains certain",
                "The Society now has a stylesheet to guide editors in the layout and");

        assertEquals(answer, SecondPartTasks.findQuotes(paths, "S"));

        assertEquals(Collections.emptyList(), SecondPartTasks.findQuotes(paths, "the jesus was black"));
    }

    @Test
    public void testPiDividedBy4() {
        final double ANSWER = Math.PI / 4;
        final double DELTA = 1e-3;
        assertEquals(ANSWER, SecondPartTasks.piDividedBy4(), DELTA);
    }

    @Test
    public void testFindPrinter() {
        final Map<String, List<String>> compositions = new HashMap<>();
        compositions.put("Aksenov", Arrays.asList("ostrov krim", "apelsini iz marokko", "kollegi"));
        compositions.put("Belyaev", Arrays.asList("anarxiya", "mat", "poryadka"));
        compositions.put("Lenin", Arrays.asList("dictatura", "proletariata"));

        assertEquals("Aksenov", SecondPartTasks.findPrinter(compositions));

        assertNull(SecondPartTasks.findPrinter(Collections.emptyMap()));
    }

    @Test
    public void testCalculateGlobalOrder() {
        final String item1 = "makaroshki";
        final String item2 = "ris";
        final String item3 = "grecha";

        final HashMap<String, Integer> order1 = new HashMap<>();
        order1.put(item1, 322);
        order1.put(item2, 228);
        order1.put(item3, 282);

        final HashMap<String, Integer> order2 = new HashMap<>();
        order2.put(item1, 1);
        order2.put(item2, 4);
        order2.put(item3, 0);

        final HashMap<String, Integer> order3 = new HashMap<>();
        order3.put(item1, 13);
        order3.put(item2, 50);
        order3.put(item3, 99);

        final HashMap<String, Integer> sumOrder = new HashMap<>();
        sumOrder.put(item1, 336);
        sumOrder.put(item2, 282);
        sumOrder.put(item3, 381);

        final List<Map<String, Integer>> orders = Arrays.asList(order1, order2, order3);

        assertEquals(sumOrder, SecondPartTasks.calculateGlobalOrder(orders));

        assertEquals(Collections.emptyMap(), SecondPartTasks.calculateGlobalOrder(Collections.emptyList()));
    }
}