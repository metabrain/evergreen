package io.github.metabrain.evergreen;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Function;

/**
 * Created by meta on 10/10/2015.
 */
public interface Evergreen<T extends Serializable> {
    /**
     * Attains the instance that is currently being stored in Evergreen.
     * @return the instance stored.
     */
    T get() throws IOException, ClassNotFoundException;


    /**
     * Atomically inserts (or replaces) the currently stored instance.
     * @param instance the instance that is to be stored.
     */
    void put(T instance) throws IOException;

    T getAndPut(Function<T, T> getAndPutFunction) throws IOException, ClassNotFoundException;
}
