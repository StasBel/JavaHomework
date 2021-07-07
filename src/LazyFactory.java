import java.util.function.Supplier;

public class LazyFactory {
    private final static Object NONE = new Object();

    public static <T> Lazy<T> createLazy1(Supplier<T> supplier) {
        return new Lazy<T>() {
            @SuppressWarnings("unchecked")
            private T result = (T) NONE;

            @Override
            public T get() {
                if (result == NONE) {
                    result = supplier.get();
                }
                return result;
            }
        };
    }

    public static <T> Lazy<T> createLazy2(Supplier<T> supplier) {
        return new Lazy<T>() {
            @SuppressWarnings("unchecked")
            private volatile T result = (T) NONE;

            @Override
            public T get() {
                if (result == NONE) {
                    synchronized (this) {
                        if (result == NONE) {
                            result = supplier.get();
                        }
                    }
                }
                return result;
            }
        };
    }

    public static <T> Lazy<T> createLazy3(Supplier<T> supplier) {
        return new AtomicTLazyHelper<>(supplier);
    }
}

