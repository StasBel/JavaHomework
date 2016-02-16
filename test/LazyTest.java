import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class LazyTest {
    private final Supplier<Integer> supplier0 = () -> 0;
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
    public void testNullCase() {
        Lazy<Integer> lazy1 = LazyFactory.createLazy1(supplierNull);
        Lazy<Integer> lazy2 = LazyFactory.createLazy2(supplierNull);
        Lazy<Integer> lazy3 = LazyFactory.createLazy2(supplierNull);
        assertEquals(lazy1.get(), lazy2.get());
        assertSame(lazy1.get(), lazy2.get());
        assertEquals(lazy2.get(), lazy3.get());
        assertSame(lazy2.get(), lazy3.get());
        assertEquals(lazy3.get(), null);
        assertSame(lazy3.get(), null);
        assertEquals(null, lazy1.get());
        assertSame(null, lazy1.get());
    }

    @Test
    public void testSimple1() {
        Lazy<Integer> lazy = LazyFactory.createLazy1(supplier0);
        assertEquals(lazy.get(), (Integer) 0);
        assertNotEquals(lazy.get(), (Integer) 1);
        assertNotEquals(lazy.get(), null);
    }

    @Test
    @SuppressWarnings("all")
    public void testHard1() {
        final int testSize = 100;
        supplier1WithCounting.setCount(0);
        Lazy<Integer> lazy = LazyFactory.createLazy1(supplier1WithCounting);
        assertSame(supplier1WithCounting.getCount(), 0);
        final Integer integer = lazy.get();
        assertEquals(integer, (Integer) 1);
        assertSame(supplier1WithCounting.getCount(), 1);
        for (int i = 0; i < testSize; i++) {
            assertSame(lazy.get(), integer);
        }
        assertSame(supplier1WithCounting.getCount(), 1);
    }

    @Test
    public void testSimple2() {
        Lazy<Integer> lazy = LazyFactory.createLazy2(supplier0);
        assertEquals(lazy.get(), (Integer) 0);
        assertNotEquals(lazy.get(), (Integer) 1);
        assertNotEquals(lazy.get(), null);
    }

    @Test
    @SuppressWarnings("all")
    public void testHard2() {
        final int testSize = 100;
        supplier1WithCounting.setCount(0);
        Lazy<Integer> lazy = LazyFactory.createLazy2(supplier1WithCounting);
        assertSame(supplier1WithCounting.getCount(), 0);
        final Integer integer = lazy.get();
        assertEquals(integer, (Integer) 1);
        assertSame(supplier1WithCounting.getCount(), 1);
        CyclicBarrier barrier = new CyclicBarrier(testSize);
        ArrayList<Thread> threadList = new ArrayList<>(testSize);
        for (int i = 0; i < testSize; i++) {
            threadList.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    assertSame(lazy.get(), integer);
                }
            }));
            threadList.get(i).start();
        }
        for (int i = 0; i < testSize; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertSame(supplier1WithCounting.getCount(), 1);
    }

    @Test
    public void testSimple3() {
        Lazy<Integer> lazy = LazyFactory.createLazy3(supplier0);
        assertEquals(lazy.get(), (Integer) 0);
        assertNotEquals(lazy.get(), (Integer) 1);
        assertNotEquals(lazy.get(), null);
    }

    @Test
    @SuppressWarnings("all")
    public void testHard3() {
        final int testSize = 100;
        supplier1WithCounting.setCount(0);
        Lazy<Integer> lazy = LazyFactory.createLazy3(supplier1WithCounting);
        assertSame(supplier1WithCounting.getCount(), 0);
        final Integer integer = lazy.get();
        assertEquals(integer, (Integer) 1);
        assertSame(supplier1WithCounting.getCount(), 1);
        CyclicBarrier barrier = new CyclicBarrier(testSize);
        ArrayList<Thread> threadList = new ArrayList<>(testSize);
        for (int i = 0; i < testSize; i++) {
            threadList.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    assertSame(lazy.get(), integer);
                }
            }));
            threadList.get(i).start();
        }
        for (int i = 0; i < testSize; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertTrue(supplier1WithCounting.getCount() >= 1);
    }
}
