# evergreen
======
Java Inter-Process Persistent Memory Mapped Objects

### Features implemented
* Uses memory mapped files (MMF);
* Thread safe Evergreen\<T extends Serializable\> object;
* Saved instance in the file is safe to read/write when accessed across multiple JVMs (by using lock bits at the target MMF);
* Low latency (but nowhere as near to OpenHFT's chronicle-map I suspect);
* Support for CAS-like operations in the MMF by specifying lambda functions;

### Example of usage

```java
String fname = "/tmp/evergreen-integer.test"
Evergreen<Integer> val = EvergreenFactory.create(fname, 4, () -> 0);

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

------------------

###  Performance
As measured on a Intel Core i3-4370 @ 3.8GHz (2 cores, 4 local threads) running 8GB DDR3, an 256GB Samsung SSD and Windows 8.
* 4 threads concurrently incrementing an Evergreen\<Integer\> 1000000 times each count using CAS mechanism, total of 4000000 operations. 

##### LATENCY (avg. ~23μs per getAndPut operation)
* (from repeatSimpleMapTestManyTimesInParallelOnSameFileCASLatencyTest)
* Considered only the latency of last 200000 operations per sample 

| Sample | Average latency (ns) | Average latency (μs) | Average latency (ms) |
| :----- | :----------------- | :----------------- | :----------------- |
| 0 | 27783ns | 27μs | 0.028ms |
| 1 | 23021ns | 23μs | 0.023ms |
| 2 | 21361ns | 21μs | 0.021ms |
| 3 | 23293ns | 23μs | 0.023ms |
| 4 | 16913ns | 16μs | 0.016ms |
| 5 | 23473ns | 23μs | 0.023ms |
| 6 | 25268ns | 25μs | 0.025ms |
| 7 | 16133ns | 16μs | 0.016ms |
| 8 | 16892ns | 16μs | 0.017ms |
| 9 | 23752ns | 23μs | 0.024ms |

##### THROUGHPUT (avg. ~145k ops per second)
* (from repeatSimpleMapTestManyTimesInParallelOnSameFileCASThroughputTest)

| Sample | Average throughput |
| :----- | :----------------- |
| 0 | 135218 ops per second |
| 1 | 155025 ops per second |
| 2 | 153028 ops per second |
| 3 | 155471 ops per second |
| 4 | 146322 ops per second |
| 5 | 148351 ops per second |
| 6 | 155572 ops per second |
| 7 | 156512 ops per second |
| 8 | 151032 ops per second |
| 9 | 146619 ops per second |

