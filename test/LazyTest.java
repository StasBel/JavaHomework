import org.junit.Test;

import java.util.ArrayList;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LazyTest {
    private final Supplier<Integer> supplier0 = () -> 0;
    private final Supplier<Integer> supplier1 = () -> 1;
    private final Supplier<Integer> supplierNull = () -> null;

    private class Supplier1WithCounting implements Supplier<Integer> {
        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public Integer get() {
            count++;
            return 1;
        }
    }

    private final Supplier1WithCounting supplier1WithCounting = new Supplier1WithCounting();

    @Test
    public void testBadCase() {
        Lazy<Integer> lazy1 = LazyFactory.createLazy1(supplierNull);
        Lazy<Integer> lazy2 = LazyFactory.createLazy2(supplierNull);
        Lazy<Integer> lazy3 = LazyFactory.createLazy2(supplierNull);
        assertEquals(lazy1.get(), lazy2.get());
        assertEquals(lazy2.get(), lazy3.get());
        assertEquals(lazy3.get(), null);
        assertEquals(null, lazy1.get());
    }

    @Test
    public void testSimple1() {
        Lazy<Integer> lazy = LazyFactory.createLazy1(supplier0);
        assertTrue(lazy.get().equals(0));
        assertFalse(lazy.get().equals(1));
    }

    @Test @SuppressWarnings("all")
    public void testHard1() {
        supplier1WithCounting.setCount(0);
        Lazy<Integer> lazy = LazyFactory.createLazy1(supplier1WithCounting);
        final Integer integer = lazy.get();
        assertTrue(integer.equals(1));
        for (int i = 0; i < 10; i++) {
            assertTrue(lazy.get() == integer);
        }
        assertTrue(supplier1WithCounting.getCount() == 1);
    }

    @Test
    public void testSimple2() {
        Lazy<Integer> lazy = LazyFactory.createLazy2(supplier0);
        assertTrue(lazy.get().equals(0));
        assertFalse(lazy.get().equals(1));
    }

    @Test @SuppressWarnings("all")
    public void testHard2() {
        supplier1WithCounting.setCount(0);
        Lazy<Integer> lazy = LazyFactory.createLazy2(supplier1WithCounting);
        final Integer integer = lazy.get();
        assertTrue(integer.equals(1));
        ArrayList<Thread> threadList = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            threadList.add(new Thread(() -> assertTrue(lazy.get() == integer)));
            threadList.get(i).start();
        }
        for (int i = 0; i < 100; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertTrue(supplier1WithCounting.getCount() == 1);
    }

    @Test
    public void testSimple3() {
        Lazy<Integer> lazy = LazyFactory.createLazy3(supplier0);
        assertTrue(lazy.get().equals(0));
        assertFalse(lazy.get().equals(1));
    }

    @Test @SuppressWarnings("all")
    public void testHard3() {
        Lazy<Integer> lazy = LazyFactory.createLazy3(supplier1);
        Integer integer = lazy.get();
        assertTrue(integer.equals(1));
        ArrayList<Thread> threadList = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            threadList.add(new Thread(() -> assertTrue(lazy.get() == integer)));
            threadList.get(i).start();
        }
        for (int i = 0; i < 100; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
