package io.github.metabrain.evergreen.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by meta on 10/10/2015.
 */
final public class UnsafeUtils {
    // prevents instantiation
    private UnsafeUtils() {
        throw new AssertionError("No instance for you!");
    }

    private static final Unsafe theUnsafe;
    static {
        Object obj = null;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            obj = f.get(null);
        } catch (Exception ignored) {}

        if(obj==null) {
            throw new RuntimeException("sun.misc.Unsafe not supported in this machine!");
        }
        theUnsafe = (Unsafe) obj;
    }

    public static Unsafe getUnsafe() {
        if(theUnsafe==null) {
            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return (Unsafe) f.get(null);
            } catch (Exception ignored) {
            }
            return null;
        }

        return theUnsafe;
    }

    public static IUnsafe getFastUnsafe() {
        return new FastUnsafe();
    }

    public static IUnsafe getSafeUnsafe() {
        return new SafeUnsafe();
    }


}
