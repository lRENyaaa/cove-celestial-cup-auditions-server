package top.rymc.phira.main.function.throwable;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@FunctionalInterface
public interface ThrowableBiPredicate<T, U, E extends Exception> {

    boolean test(T t, U u) throws E;

    default ThrowableBiPredicate<T, U, E> and(ThrowableBiPredicate<? super T, ? super U, ? extends E> other) {
        Objects.requireNonNull(other);
        return (T t, U u) -> test(t, u) && other.test(t, u);
    }

    default ThrowableBiPredicate<T, U, E> and(BiPredicate<? super T, ? super U> other) {
        Objects.requireNonNull(other);
        return (T t, U u) -> test(t, u) && other.test(t, u);
    }

    default ThrowableBiPredicate<T, U, E> negate() {
        return (T t, U u) -> !test(t, u);
    }

    default ThrowableBiPredicate<T, U, E> or(ThrowableBiPredicate<? super T, ? super U, ? extends E> other) {
        Objects.requireNonNull(other);
        return (T t, U u) -> test(t, u) || other.test(t, u);
    }

    default ThrowableBiPredicate<T, U, E> or(BiPredicate<? super T, ? super U> other) {
        Objects.requireNonNull(other);
        return (T t, U u) -> test(t, u) || other.test(t, u);
    }

    default BiPredicate<T, U> toUncheckedBiPredicate() {
        return toBiPredicate((e) -> {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        });
    }

    default BiPredicate<T, U> toBiPredicate(Predicate<Exception> onError) {
        return (t, u) -> {
            try {
                return test(t, u);
            } catch (Exception e) {
                return onError.test(e);
            }
        };
    }
}
