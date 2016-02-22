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
                    result = supplier.get();
                    isAlreadyCalculated = true;
                }
                return result;
            }
        };
    }

    public static <T> Lazy<T> createLazy2(Supplier<T> supplier) {
        return new Lazy<T>() {
            private final Object NONE = new Object();
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
        class AtomicTLazyHelper implements Lazy<T> {
            @SuppressWarnings("all")
            private volatile T result;
            private final AtomicReferenceFieldUpdater<AtomicTLazyHelper, Object> updater =
                    AtomicReferenceFieldUpdater.newUpdater(AtomicTLazyHelper.class, Object.class, "result");

            @Override
            @SuppressWarnings("unchecked")
            public T get() {
                updater.compareAndSet(this, result, supplier.get());
                return result;
            }
        }

        return new AtomicTLazyHelper();
    }
}
