import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

public class LazyFactory {
    public static <T> Lazy<T> createLazy1(Supplier<T> supplier) {
        return new Lazy<T>() {
            private boolean isAlreadyCalculated = false;
            private T result;

            @Override
            public T get() {
                if (!isAlreadyCalculated) {
                    isAlreadyCalculated = true;
                    result = supplier.get();
                }
                return result;
            }
        };
    }

    public static <T> Lazy<T> createLazy2(Supplier<T> supplier) {
        return new Lazy<T>() {
            private final Lazy<T> lazy = LazyFactory.createLazy1(supplier);

            @Override
            public synchronized T get() {
                return lazy.get();
            }
        };
    }

    public static <T> Lazy<T> createLazy3(Supplier<T> supplier) {
        class AtomicLazyHelper implements Lazy<T> {
            private volatile Lazy lazy = LazyFactory.createLazy1(supplier);
            private final AtomicReferenceFieldUpdater<AtomicLazyHelper, Lazy> updater =
                    AtomicReferenceFieldUpdater.newUpdater(AtomicLazyHelper.class, Lazy.class, "lazy");

            @Override @SuppressWarnings("unchecked")
            public T get() {
                return (T) updater.get(this).get();
            }
        }

        return new AtomicLazyHelper();
    }
}
