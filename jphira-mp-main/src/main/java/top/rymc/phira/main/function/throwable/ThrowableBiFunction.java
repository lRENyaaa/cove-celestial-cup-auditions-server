package top.rymc.phira.main.function.throwable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface ThrowableBiFunction<T, U, R, E extends Exception> {

    R apply(T t, U u) throws E;


    default <V> ThrowableBiFunction<T, U, V, E> andThen(ThrowableFunction<? super R, ? extends V, E> after) {
        Objects.requireNonNull(after);
        return (t, u) -> after.apply(apply(t, u));
    }

    default <V> ThrowableBiFunction<T, U, V, E> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(apply(t, u));
    }


    default BiFunction<T, U, R> toUncheckedBiFunction() {
        return toBiFunction((e) -> {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        });
    }

    default BiFunction<T, U, R> toBiFunction(Function<Exception, R> onError) {
        return (t, u) -> {
            try {
                return apply(t, u);
            } catch (Exception e) {
                return onError.apply(e);
            }
        };
    }

}
