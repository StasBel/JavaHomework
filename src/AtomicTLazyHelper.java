import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

class AtomicTLazyHelper<T> implements Lazy<T> {
    private final static Object NONE = new Object();

    private final Supplier<T> supplier;
    @SuppressWarnings("unused, unchecked")
    private volatile T result = (T) NONE;
    private final static AtomicReferenceFieldUpdater<AtomicTLazyHelper, Object> updater =
            AtomicReferenceFieldUpdater.newUpdater(AtomicTLazyHelper.class, Object.class, "result");

    public AtomicTLazyHelper(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        if (result == NONE) {
            updater.compareAndSet(this, NONE, supplier.get());
        }

        return result;
    }
}
