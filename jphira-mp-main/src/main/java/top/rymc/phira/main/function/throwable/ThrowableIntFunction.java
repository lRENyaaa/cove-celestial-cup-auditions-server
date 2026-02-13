package top.rymc.phira.main.function.throwable;

import java.util.function.Function;
import java.util.function.IntFunction;

@FunctionalInterface
public interface ThrowableIntFunction<R, E extends Exception> {
    R apply(int value) throws E;

    default IntFunction<R> toUncheckedIntFunction() {
        return toIntFunction((e) -> {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        });
    }

    default IntFunction<R> toIntFunction(Function<Exception, R> onError) {
        return (t) -> {
            try {
                return apply(t);
            } catch (Exception e) {
                return onError.apply(e);
            }
        };
    }
}