import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

class AtomicTLazyHelper<T> implements Lazy<T> {
    private final static Object NONE = new Object();
    private final static AtomicReferenceFieldUpdater<AtomicTLazyHelper, Object> UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AtomicTLazyHelper.class, Object.class, "result");

    private final Supplier<T> supplier;
    @SuppressWarnings("unused, unchecked")
    private volatile T result = (T) NONE;

    public AtomicTLazyHelper(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (result == NONE) {
            UPDATER.compareAndSet(this, NONE, supplier.get());
        }

        return result;
    }
}
