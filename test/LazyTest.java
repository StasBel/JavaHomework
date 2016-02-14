import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;

public class LazyTest {
    private static final Supplier<Integer> supplier0 = () -> 0;
    private static final Supplier<Integer> supplier1 = () -> 1;
    private static final Supplier<Integer> supplierNull = () -> null;

    @Test
    public void testSimple() {
        Lazy<Integer> lazy = LazyFactory.createLazy1(supplier0);
        assertTrue(lazy.get().equals(0));
    }
}
