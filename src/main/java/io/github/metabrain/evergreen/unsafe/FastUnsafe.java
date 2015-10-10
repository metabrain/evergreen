package io.github.metabrain.evergreen.unsafe;

import sun.misc.Unsafe;

/**
 * Created by meta on 10/10/2015.
 */
public class FastUnsafe implements IUnsafe {

    protected FastUnsafe() {}

    @Override
    public int getInt(long address) {
        return theUnsafe.getInt(null, address);
    }

    @Override
    public void putInt(long address, int val) {
        theUnsafe.putInt(null, address, val);
    }

}
