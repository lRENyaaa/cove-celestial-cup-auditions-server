package top.rymc.phira.main.function.throwable;


import java.util.Objects;
import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowableConsumer <T, E extends Exception> {

    void accept(T t) throws E;

    default ThrowableConsumer<T, E> andThen(ThrowableConsumer<? super T, ? extends E> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }

    default ThrowableConsumer<T, E> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }

    default Consumer<T> toUncheckedConsumer() {
        return toConsumer((e) -> {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        });
    }

    default Consumer<T> toConsumer(Consumer<Exception> onError) {
        return (t) -> {
            try {
                accept(t);
            } catch (Exception e) {
                onError.accept(e);
            }
        };
    }

}
