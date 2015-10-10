package io.github.metabrain.evergreen;

import io.github.metabrain.evergreen.unsafe.IUnsafe;
import io.github.metabrain.evergreen.unsafe.UnsafeUtils;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * Created by meta on 10/10/2015.
 */

public final class EvergreenImpl<T extends Serializable> implements Evergreen<T> {

    // SPECIAL CONSTANTS
    private static final int LOCKED = (int)1;
    private static final int UNLOCKED = (int)0;

    // OFFSETS FOR MEMORY MAPPED FILE
    private static final int LOCK_OFFSET = 0x0;
//        private static final int MAX_SIZE_OFFSET = 0x4;
    // private static final int FREE_HEADER_FLD_OFFSET = 0x8;
    // private static final int FREE_HEADER_FLD_OFFSET = 0xB;

    private static final int OBJ_START_OFFSET = 0x10; //0xF (16) bytes for total header and then object bytes start.

    private static final int HEADER_SIZE = OBJ_START_OFFSET-1;

    private final int bufSize;
    private final MappedByteBuffer mmb;
    private final long lockByteOffHeapAddress;
    private final byte[] buf;

    private static IUnsafe theUnsafe;

    // TODO THE HEADERS:
    // TODO locked bit
    // TODO initialized bit
    // TODO bufSize of the file stored to ensure no two instances are pointing to same file and requested different sizes

    protected EvergreenImpl(FileChannel fc, int maxInstanceSizeInBytes, Supplier<T> initializer) throws IOException {
        this(fc, maxInstanceSizeInBytes, initializer, true);
    }

    protected EvergreenImpl(FileChannel fc, int maxInstanceSizeInBytes, Supplier<T> initializer, boolean safe) throws IOException {
        if(safe) {
            theUnsafe = UnsafeUtils.getSafeUnsafe();
        } else {
            // maximum throughput, operations are not-volatile hence not mandatory to be seen by other concurrent threads.
            theUnsafe = UnsafeUtils.getFastUnsafe();
        }

        bufSize = HEADER_SIZE+maxInstanceSizeInBytes+128/*class declaration overhead etc*/;
        this.buf = new byte[bufSize];
        this.mmb = fc.map(FileChannel.MapMode.READ_WRITE, 0, bufSize+16/*class header size??*/);

        this.lockByteOffHeapAddress = ((DirectBuffer) mmb).address()+LOCK_OFFSET;

        try {
            lock();
            // Try to get the preexisting instance in the file. If it doesn't exist, insert a newly created one.
            try {
                get0(false);
            } catch (Exception e) {
//                System.out.println("Previous instance did not exist or was corrupted. Assigning new one...");
                put0(initializer.get(), false);
            }
        } finally {
            unlock();
        }
    }

    public synchronized T get() throws IOException, ClassNotFoundException {
        return get0(true);
    }

    public synchronized void put(T instance) throws IOException {
        put0(instance, true);
    }

    public synchronized T getAndPut(Function<T, T> getAndPutFunction) throws IOException, ClassNotFoundException {
        return getAndPut0(getAndPutFunction);
    }

    private void lock() {
        boolean casSucess = false;
        while(!casSucess) {
            final int originalLockByteVal = theUnsafe.getInt(lockByteOffHeapAddress);
            if(originalLockByteVal!=UNLOCKED) {
                // Need to wait till its unlocked...
//                    System.out.println("WAS LOCKED!!!");
                continue;
            }
            casSucess = theUnsafe.compareAndSwapInt(lockByteOffHeapAddress, originalLockByteVal, LOCKED);
            if(!casSucess) {
//                    System.out.println("CAS FAILED!!!");
            }
        }
    }

    private void unlock() {
        // no need to check previous value since it was locked to us so nobody could have touched it... In theory...
        theUnsafe.putInt(lockByteOffHeapAddress, UNLOCKED);
    }

    private T get0(boolean shouldLock) throws IOException, ClassNotFoundException {
        mmb.position(OBJ_START_OFFSET);
        if(shouldLock) {
            try {
                lock();
                mmb.get(buf);
            } finally {
                // Ensure we always leave it unlocked no mather what happens
                unlock();
            }
        } else {
            mmb.get(buf);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        ObjectInputStream is = new ObjectInputStream(in);
        return (T) is.readObject();
    }

    public void put0(T instance, boolean shouldLock) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(instance);
        mmb.position(OBJ_START_OFFSET);

        if(shouldLock) {
            try {
                lock();
                mmb.put(out.toByteArray());
            } finally {
                // Ensure we always leave it unlocked no mather what happens
                unlock();
            }
        } else {
            mmb.put(out.toByteArray());
        }
    }

    public T getAndPut0(Function<T, T> getAndPutFunction) throws IOException, ClassNotFoundException {
        lock();
        T got = get0(false);
        T result = getAndPutFunction.apply(got);
        put0(result, false);
        unlock();
        return result;
    }

}
