package io.github.metabrain.evergreen.unsafe;

import sun.misc.Unsafe;

/**
 * Created by meta on 10/10/2015.
 */
public class SafeUnsafe implements IUnsafe {

    protected SafeUnsafe() {}

    @Override
    public int getInt(long address) {
        return theUnsafe.getIntVolatile(null, address);
    }

    @Override
    public void putInt(long address, int val) {
        theUnsafe.putIntVolatile(null, address, val);
    }

}
