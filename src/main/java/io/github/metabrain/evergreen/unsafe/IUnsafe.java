package io.github.metabrain.evergreen.unsafe;

import sun.misc.Unsafe;

/**
 * Created by meta on 10/10/2015.
 */
public interface IUnsafe {
    Unsafe theUnsafe = UnsafeUtils.getUnsafe();

    int getInt(long address);
    void putInt(long address, int val);
    default boolean compareAndSwapInt(long address, int oldVal, int newVal) {
        return theUnsafe.compareAndSwapInt(null, address, oldVal, newVal);
    }
}
