import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class LazyTest {
    private static final int TEST_SIZE = 200;

    private class Supplier1WithCounting implements Supplier<Integer> {
        private final AtomicInteger count = new AtomicInteger(0);

        public void reset() {
            count.set(0);
        }

        public int getCount() {
            return count.get();
        }

        @Override
        public Integer get() {
            count.incrementAndGet();
            return 1;
        }
    }

    private final Supplier1WithCounting supplier1WithCounting = new Supplier1WithCounting();

    @Test
    public void testHard1() {
        supplier1WithCounting.reset();
        Lazy<Integer> lazy = LazyFactory.createLazy1(supplier1WithCounting);
        assertSame(supplier1WithCounting.getCount(), 0);
        final Integer integer = lazy.get();
        assertEquals(integer, (Integer) 1);
        assertSame(supplier1WithCounting.getCount(), 1);
        for (int i = 0; i < TEST_SIZE; i++) {
            assertSame(lazy.get(), integer);
        }
        assertSame(supplier1WithCounting.getCount(), 1);
    }

    private void testLazyInteger(Lazy<Integer> lazy) {
        assertSame(supplier1WithCounting.getCount(), 0);
        final Integer integer = lazy.get();
        assertEquals(integer, (Integer) 1);
        assertSame(supplier1WithCounting.getCount(), 1);
        CyclicBarrier barrier = new CyclicBarrier(TEST_SIZE);
        ArrayList<Thread> threadList = new ArrayList<>(TEST_SIZE);
        for (int i = 0; i < TEST_SIZE; i++) {
            threadList.add(new Thread(() -> {
                try {
                    barrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                assertSame(lazy.get(), integer);
            }));
            threadList.get(i).start();
        }
        for (int i = 0; i < TEST_SIZE; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testHard2() {
        supplier1WithCounting.reset();
        Lazy<Integer> lazy = LazyFactory.createLazy2(supplier1WithCounting);
        testLazyInteger(lazy);
        assertSame(supplier1WithCounting.getCount(), 1);
    }

    @Test
    @SuppressWarnings("all")
    public void testHard3() {
        supplier1WithCounting.reset();
        Lazy<Integer> lazy = LazyFactory.createLazy3(supplier1WithCounting);
        testLazyInteger(lazy);
        assertTrue(supplier1WithCounting.getCount() >= 1);
    }
}
