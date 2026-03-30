package com.market360.core.result;

import java.util.Optional;
import java.util.function.Function;

/**
 * Type résultat sealed pour les agents — jamais de null retourné.
 *
 * @param <T> type de la valeur en cas de succès
 */
public sealed interface AgentResult<T> {

    record Success<T>(T value) implements AgentResult<T> {}

    record Failure<T>(String reason, Throwable cause) implements AgentResult<T> {}

    /** Retourne la valeur ou lève une RuntimeException */
    default T orElseThrow() {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw new AgentException(f.reason(), f.cause());
        };
    }

    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T> s -> Optional.of(s.value());
            case Failure<T> f -> Optional.empty();
        };
    }

    default <U> AgentResult<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T> s -> new Success<>(mapper.apply(s.value()));
            case Failure<T> f -> new Failure<>(f.reason(), f.cause());
        };
    }

    static <T> AgentResult<T> success(T value) {
        return new Success<>(value);
    }

    static <T> AgentResult<T> failure(String reason, Throwable cause) {
        return new Failure<>(reason, cause);
    }

    static <T> AgentResult<T> failure(String reason) {
        return new Failure<>(reason, null);
    }
}
