package top.rymc.phira.main.function.throwable;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ThrowableFunction<T, R, E extends Exception> {

    R apply(T t) throws E;

    default <V> ThrowableFunction<T,V,E> andThen(ThrowableFunction<? super R, ? extends V, E> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    default <V> ThrowableFunction<T,V,E> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    default <V> ThrowableFunction<V,R,E> compose(ThrowableFunction<? super V, ? extends T, E> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    default <V> ThrowableFunction<V,R,E> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    static <T, E extends Exception> ThrowableFunction<T,T,E> identity() {
        return t -> t;
    }

    default Function<T, R> toUncheckedFunction() {
        return toFunction((e) -> {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        });
    }

    default Function<T, R> toFunction(Function<Exception, R> onError) {
        return (t) -> {
            try {
                return apply(t);
            } catch (Exception e) {
                return onError.apply(e);
            }
        };
    }
}
