import io.github.metabrain.evergreen.Evergreen;
import io.github.metabrain.evergreen.EvergreenFactory;
import junit.framework.Assert;
import org.junit.Test;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.LambdaMetafactory;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by meta on 10/10/2015.
 */
public class SimpleTest {

    private final static int N_CORES = Runtime.getRuntime().availableProcessors();

    @Test
    public void simpleIntegerCASTest() throws IOException, ClassNotFoundException {
        final int totalIncrements = 10000;

        // Create file for tests
        File f = File.createTempFile("EvergreenTestFile_"+ UUID.randomUUID(),"mmf");
        String fname = f.getAbsolutePath();

        Evergreen<Integer> mmo = EvergreenFactory.create(fname, 4, () -> 0);

        int integer = -1;
        // increments per thread
        for(int op=0 ; op<totalIncrements ; op++) {
//            System.out.println("GetandPut...");
            integer = mmo.getAndPut((got) -> got+1);
//            System.out.println("Currently on integer "+integer);
        }

        Assert.assertEquals(totalIncrements, integer);
    }

    @Test
    public void simpleIntegerTest() throws IOException, ClassNotFoundException {
        Integer i = putAndGet(4, 0, 2);

        Assert.assertEquals(2, (int)i);
    }

    @Test
    public void simpleMapTest() throws IOException, ClassNotFoundException {
        final int mapSize = 10;

        HashMap<String, String> map = new HashMap<>(mapSize);
        for(int i=0; i<mapSize ; i++) {
            map.put("k-"+i,"v-"+i);
        }

        Map<String, String> newMap = putAndGet(mapSize*10+1024, new HashMap<>(), map);

        assertEquals(map, newMap);
    }

    @Test
    public void repeatSimpleIntegerTestManyTimes() throws IOException, ClassNotFoundException {
        for(int i=0 ; i<100 ; i++) {
            simpleIntegerTest();
        }
    }

    @Test
    public void repeatSimpleMapTestManyTimes() throws IOException, ClassNotFoundException {
        for(int i=0 ; i<100 ; i++) {
            simpleMapTest();
        }
    }


    @Test
    public void repeatSimpleMapTestManyTimesInParallel() throws IOException, ClassNotFoundException, InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(N_CORES);
        CountDownLatch endLatch = new CountDownLatch(N_CORES);
        Runnable fun = () -> {
            startLatch.countDown();
            try {
                startLatch.await();
                for (int i = 0; i < 1000; i++) {
                    try {
                        simpleMapTest();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                endLatch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        for(int i=0 ; i<N_CORES ; i++) {
            new Thread(fun).start();
        }

        // Wait till all threads finish
        endLatch.await();
    }


    @Test
    public void repeatSimpleMapTestManyTimesInParallelOnSameFileCAS() throws IOException, ClassNotFoundException, InterruptedException {
        int incrementsPerCore = 1000000;

        // Create file for tests
        File f = File.createTempFile("EvergreenTestFile_"+ UUID.randomUUID(),"mmf");
        String fname = f.getAbsolutePath();

        CountDownLatch startLatch = new CountDownLatch(N_CORES);
        CountDownLatch endLatch = new CountDownLatch(N_CORES);
        Runnable fun = () -> {
            Thread.currentThread().setName("Evergreen test thread on "+new Exception().getStackTrace()[0].getMethodName());
            startLatch.countDown();
            try {
                startLatch.await();

                Evergreen<Integer> mmo = EvergreenFactory.create(fname, 4, () -> 0);

                // increments per thread
                for(int op=0 ; op<incrementsPerCore ; op++) {
//                    System.out.println("GetandPut...");
                    long startCAS = System.nanoTime();
                    int integer = mmo.getAndPut((got) -> got+1);
                    long endCAS = System.nanoTime();
                    if(integer > incrementsPerCore/2) {
                        System.out.println("getAndPut()  took " + (endCAS - startCAS) + "ns.");
                    }
//                    System.out.println("Currently on integer "+integer);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        };

        for(int i=0 ; i<N_CORES ; i++) {
            new Thread(fun).start();
        }

        System.out.println("Waiting till all threads finish...");
        // Wait till all threads finish
        endLatch.await();

        // Get stored result
        Evergreen<Integer> mmo = EvergreenFactory.create(fname, 4, () -> 0);
        int result = mmo.get();

        assertEquals(N_CORES*incrementsPerCore, result);

        f.delete();
    }

    @Test
    public void repeatableSimpleMapTestManyTimesInParallelOnSameFileCASThroughputTest() throws IOException, ClassNotFoundException, InterruptedException {
        while(true) {
            repeatSimpleMapTestManyTimesInParallelOnSameFileCASThroughputTest();
        }
    }

    @Test
    public void repeatSimpleMapTestManyTimesInParallelOnSameFileCASThroughputTest() throws IOException, ClassNotFoundException, InterruptedException {
        int incrementsPerCore = 1000000;

        // Create file for tests
        File f = File.createTempFile("EvergreenTestFile_"+ UUID.randomUUID(),"mmf");
        String fname = f.getAbsolutePath();

        AtomicLong total = new AtomicLong(0);
        AtomicLong measuredOps = new AtomicLong(0);

        CountDownLatch startLatch = new CountDownLatch(N_CORES);
        CountDownLatch endLatch = new CountDownLatch(N_CORES);
        Runnable fun = () -> {
            Thread.currentThread().setName("Evergreen test thread on "+new Exception().getStackTrace()[0].getMethodName());
            startLatch.countDown();
            try {
                startLatch.await();

                Evergreen<Integer> mmo = EvergreenFactory.create(fname, 4, () -> 0);

                // increments per thread
                for(int op=0 ; op<incrementsPerCore ; op++) {
//                    System.out.println("GetandPut...");
                    int integer = mmo.getAndPut((got) -> got+1);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        };

        long startCAS = System.nanoTime();
        for(int i=0 ; i<N_CORES ; i++) {
            new Thread(fun).start();
        }

        // Wait till all threads finish
        endLatch.await();
        long endCAS = System.nanoTime();

        // Get stored result
        Evergreen<Integer> mmo = EvergreenFactory.create(fname, 4, () -> 0);
        int result = mmo.get();

        assertEquals(N_CORES*incrementsPerCore, result);

        double elapsedSecs = ((endCAS-startCAS)/1000000000.0);
        long throughput = (long)((double)result/elapsedSecs);
        System.out.println("Average throughput of "+throughput+" ops per seconds");

        f.delete();
    }


//    @Test
//    public void repeatableSimpleMapTestManyTimesInParallelOnSameFileCASLatencyTest() throws IOException, ClassNotFoundException, InterruptedException {
//        while(true) {
//            repeatSimpleMapTestManyTimesInParallelOnSameFileCASLatencyTest();
//        }
//    }

    @Test
    public void repeatSimpleMapTestManyTimesInParallelOnSameFileCASLatencyTest() throws IOException, ClassNotFoundException, InterruptedException {
        int incrementsPerCore = 1000000;

        // Create file for tests
        File f = File.createTempFile("EvergreenTestFile_"+ UUID.randomUUID(),"mmf");
        String fname = f.getAbsolutePath();

        AtomicLong total = new AtomicLong(0);
        AtomicLong measuredOps = new AtomicLong(0);

        CountDownLatch startLatch = new CountDownLatch(N_CORES);
        CountDownLatch endLatch = new CountDownLatch(N_CORES);
        Runnable fun = () -> {
            Thread.currentThread().setName("Evergreen test thread on "+new Exception().getStackTrace()[0].getMethodName());
            startLatch.countDown();
            try {
                startLatch.await();

                Evergreen<Integer> mmo = EvergreenFactory.create(fname, 4, () -> 0);

                // increments per thread
                for(int op=0 ; op<incrementsPerCore ; op++) {
//                    System.out.println("GetandPut...");
                    long startCAS = System.nanoTime();
                    int integer = mmo.getAndPut((got) -> got+1);
                    long endCAS = System.nanoTime();
                    if(integer > (incrementsPerCore*N_CORES)*0.95) {
                        long elapsed = (endCAS - startCAS);
//                        System.out.println("getAndPut()  took " + elapsed + "ns.");
                        total.addAndGet(elapsed);
                        measuredOps.incrementAndGet();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        };

        for(int i=0 ; i<N_CORES ; i++) {
            new Thread(fun).start();
        }

        // Wait till all threads finish
        endLatch.await();

        // Get stored result
        Evergreen<Integer> mmo = EvergreenFactory.create(fname, 4, () -> 0);
        int result = mmo.get();

        assertEquals(N_CORES*incrementsPerCore, result);

        System.out.println("Average latency of last "+measuredOps+" operations done: "
                +(long)((double)total.get()/(double)measuredOps.get())+"ns / "
                +(long)((double)(total.get()/1000.0)/(double)measuredOps.get())+"μs / "
                +new DecimalFormat("0.000").format((double)((double)(total.get()/1000000.0)/(double)measuredOps.get()))+"ms");

        f.delete();
    }

    public static <T extends Serializable> T putAndGet(int size, T initial, T put) throws IOException, ClassNotFoundException {
        // Create file for tests
        File f = File.createTempFile("EvergreenTestFile_"+ UUID.randomUUID(),"mmf");

        String fname = f.getAbsolutePath();

        Evergreen<T> mmo = EvergreenFactory.create(fname, size, () -> initial);

        long putStart = System.nanoTime();
        mmo.put(put);
        long putEnd = System.nanoTime();
//        System.out.println("put() for '"+put.getClass().getSimpleName()+":"+put.hashCode()+"' object took "+(putEnd-putStart)+"ns.");

        long getStart = System.nanoTime();
        T got = mmo.get();
        long getEnd = System.nanoTime();
//        System.out.println("get() for '"+got.getClass().getSimpleName()+":"+got.hashCode()+"' object took "+(getEnd-getStart)+"ns.");

        f.delete();

        return got;
    }
}
