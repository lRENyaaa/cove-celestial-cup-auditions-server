package top.rymc.race.function.throwable;

@FunctionalInterface
public interface ThrowableIntFunction<R, E extends Exception> {
    R apply(int value) throws E;
}