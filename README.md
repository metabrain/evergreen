# evergreen
======
Java Concurrent Persistent Memory Mapped Objects (one object per file)

### Features implemented
* Uses memory mapped files (MMF);
* Thread safe Evergreen<T extends Serializable> object;
* Safe when accessed across multiple JVMs (by using lock bits at the target MMF);
* Low latency (but nowhere as near to OpenHFT chronicle-map I suspect);
* Support for CAS-like operations in the MMF by specifying lambda functions;

### Example of usage

```java
String fname = "/tmp/evergreen-integer.test"
Evergreen\<Integer\> val = EvergreenFactory.create(fname, 4, () -> 0);

// Returns the current value as currently available in memory-mapped file.
Integer savedInt = val.get()
// NOTE: Initial value will be "0" in this case, since the file didn't exist so we used the "() -> 0" lambda to seed the instance in the file.

// Inserts a new instance in the memory-mapped file, immediately available to other threads/processes/JVMs.
val.put(new Integer(5)) 

// Performs an atomic operation on the instance on the memory-mapped-file.
// The lambda is only executed once when the instance is locked and safe to edit. As such, it can have side-effects.
Integer newSavedInt = val.getAndPut((oldInt) -> oldInt+1)) 
// NOTE: Lambda cannot contain operations on the same Evergreen object, or a deadlock will occur!
```

### Things that would be nice to add in the future if possible (TODO)
* No garbage generating implementation. (to be fair, I don't know how much garbage is currently generated).
* Add support for forceClearLock() (in the case a JVM crashes while a file is locked, which would render it un-unlockable without forcing).
* Allow users to specify mroe complex (and faster) serialization mechanisms.
* Understand better the values for sizes of headers in Java objects in order to avoid overestimating it.
* Add support for multiple concurrent readers (no locking when reading) but prevent writer starvation. 

###  PERFORMANCE
As measured on a Intel Core i3-4370 @ 3.8GHz (2 cores, 4 local threads) running 8GB DDR3, an 256GB Samsung SSD and Windows 8.

##### LATENCY
* 4 threads concurrently incrementing an Evergreen<Integer> 1000000 times each count using CAS mechanism, total of 4000000 operations. (repeatSimpleMapTestManyTimesInParallelOnSameFileCASLatencyTest)

Sample 0 : Average latency of last 200000 operations done: 27783ns / 27μs / 0.028ms
Sample 1 : Average latency of last 200000 operations done: 23021ns / 23μs / 0.023ms
Sample 2 : Average latency of last 200000 operations done: 21361ns / 21μs / 0.021ms
Sample 3 : Average latency of last 200000 operations done: 23293ns / 23μs / 0.023ms
Sample 4 : Average latency of last 200000 operations done: 16913ns / 16μs / 0.016ms
Sample 5 : Average latency of last 200000 operations done: 23473ns / 23μs / 0.023ms
Sample 6 : Average latency of last 200000 operations done: 25268ns / 25μs / 0.025ms
Sample 7 : Average latency of last 200000 operations done: 16133ns / 16μs / 0.016ms
Sample 8 : Average latency of last 200000 operations done: 16892ns / 16μs / 0.017ms
Sample 9 : Average latency of last 200000 operations done: 23752ns / 23μs / 0.024ms

##### THROUGHPUT
* 4 threads concurrently incrementing an Evergreen<Integer> 1000000 times each count using CAS mechanism, total of 4000000 operations. (repeatSimpleMapTestManyTimesInParallelOnSameFileCASThroughputTest)
Sample 0 : Average throughput of 135218 ops per seconds
Sample 1 : Average throughput of 155025 ops per seconds
Sample 2 : Average throughput of 153028 ops per seconds
Sample 3 : Average throughput of 155471 ops per seconds
Sample 4 : Average throughput of 146322 ops per seconds
Sample 5 : Average throughput of 148351 ops per seconds
Sample 6 : Average throughput of 155572 ops per seconds
Sample 7 : Average throughput of 156512 ops per seconds
Sample 8 : Average throughput of 151032 ops per seconds
Sample 9 : Average throughput of 146619 ops per seconds

