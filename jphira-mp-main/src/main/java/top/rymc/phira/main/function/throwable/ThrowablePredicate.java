package top.rymc.phira.main.function.throwable;

import java.util.Objects;
import java.util.function.Predicate;

public interface ThrowablePredicate<T, E extends Exception> {

    boolean test(T t) throws E;

    default ThrowablePredicate<T, E> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    default ThrowablePredicate<T, E> and(ThrowablePredicate<? super T, ? extends E> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    default ThrowablePredicate<T, E> negate() {
        return (t) -> !test(t);
    }

    default ThrowablePredicate<T, E> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    default ThrowablePredicate<T, E> or(ThrowablePredicate<? super T, ? extends E> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    @SuppressWarnings("unchecked")
    static <T, E extends Exception> ThrowablePredicate<T, E> not(ThrowablePredicate<? super T, E> target) {
        Objects.requireNonNull(target);
        return (ThrowablePredicate<T, E>) target.negate();
    }

    default Predicate<T> toUncheckedPredicate() {
        return toPredicate((e) -> {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        });
    }

    default Predicate<T> toPredicate(Predicate<Exception> onError) {
        return (t) -> {
            try {
                return test(t);
            } catch (Exception e) {
                return onError.test(e);
            }
        };
    }
}
