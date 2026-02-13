package top.rymc.phira.main.function.throwable;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowableSupplier<T, E extends Exception>  {

    T get() throws E;

    default Supplier<T> toUncheckedSupplier() {
        return toSupplier((e) -> {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        });
    }

    default Supplier<T> toSupplier(Function<Exception, T> onError) {
        return () -> {
            try {
                return get();
            } catch (Exception e) {
                return onError.apply(e);
            }
        };
    }
}
