package top.rymc.phira.main.function.throwable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowableBiConsumer<T, U, E extends Exception>  {

    void accept(T t, U u) throws E;

    default ThrowableBiConsumer<T, U, E> andThen(BiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }

    default ThrowableBiConsumer<T, U, E> andThen(ThrowableBiConsumer<? super T, ? super U, ? extends E> after) {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }

    default BiConsumer<T, U> toUncheckedBiConsumer() {
        return toBiConsumer((e) -> {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        });
    }

    default BiConsumer<T, U> toBiConsumer(Consumer<Exception> onError) {
        return (t, u) -> {
            try {
                accept(t, u);
            } catch (Exception e) {
                onError.accept(e);
            }
        };
    }
}
