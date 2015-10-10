package io.github.metabrain.evergreen;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by meta on 10/10/2015.
 */
final public class EvergreenFactory {
    // prevents instantiation
    private EvergreenFactory() {
        throw new AssertionError("No instance for you!");
    }

    /** No garbage created approach. The object returned is cached and updated when get(...) is used.
     *
     * @param <T> class the object that will get saved on the file.
     * @param filepath
     * @param maxInstanceSizeInBytes
     * @param initializer lambda function that will provide a clean instance of the object in case none exists yet.
     * @return the previously mapped instance or a new instance constructed using the initializer supplied, in case none exists yet.
     * @throws IOException
     */
    public static <T extends Serializable> EvergreenImpl<T> create(String filepath, int maxInstanceSizeInBytes, Supplier<T> initializer) throws IOException{
        // Create the memory mapped file to be used to represent this object
        File f = new File(filepath);
        if(!f.exists()) {
            if(!f.createNewFile()) {
                throw new IOException("File '"+filepath+"' creation failed.");
            }
        }

        FileChannel fc = new RandomAccessFile(f, "rw").getChannel();

        return new EvergreenImpl<>(fc, maxInstanceSizeInBytes, initializer);
    }
}


