# Cache & Memory Hierarchy Simulator (Java)

A configurable cache simulator for exploring how cache parameters impact performance.
Supports an L1 cache with an optional L2 cache and reports detailed statistics from memory-access traces.

## Features
- Configurable block size, cache size, and associativity (L1 and optional L2)
- Replacement policies: LRU, FIFO, Optimal
- Write policy: write-back + write-allocate
- Cache hierarchy behavior: non-inclusive and inclusive
- Prints cache contents and key performance stats:
  - reads/writes, read/write misses
  - miss rates
  - writebacks
  - total memory traffic

## Project Structure
.
├── src
│   ├── sim_cache.java
│   └── CacheDisplay.java
├── Makefile
└── README.md

## Requirements
- Java JDK 8+ (11+ recommended)
- make (optional, but recommended)

Check your Java version:
java -version

## Build
Compile and generate the ./sim_cache launcher:
make

Clean build artifacts:
make clean

## Run
The simulator expects exactly 8 command-line arguments:

./sim_cache <BLOCKSIZE> <L1_SIZE> <L1_ASSOC> <L2_SIZE> <L2_ASSOC> <REPLACEMENT_POLICY> <INCLUSION_PROPERTY> <trace_file>

### Argument Details
- BLOCKSIZE: block size in bytes
- L1_SIZE: L1 cache size in bytes
- L1_ASSOC: L1 associativity
- L2_SIZE: L2 cache size in bytes (0 means “no L2”)
- L2_ASSOC: L2 associativity
- REPLACEMENT_POLICY:
  - 0 = LRU
  - 1 = FIFO
  - 2 = Optimal
- INCLUSION_PROPERTY:
  - 0 = non-inclusive
  - 1 = inclusive
- trace_file: memory access trace file

### Example
./sim_cache 32 8192 4 262144 8 0 0 traces/gcc_trace.txt

## Trace Format
Each line in the trace is:
r|w <hex_address>

Example:
r 0x7ffdf0a0
w 0x0040a1b2

## Output
The program prints:
- cache configuration summary
- cache contents (per level)
- final statistics such as:
  - number of reads/writes
  - read/write misses and miss rates
  - writebacks
  - total memory traffic

## Notes for a Public Repo
- Don’t commit trace files if they’re large or not redistributable.
- .gitignore should exclude Java build artifacts like *.class.

## Ideas for Future Improvements
- Add automated tests using small synthetic traces
- Add CSV/JSON output mode for easier plotting and analysis
- Add support for additional write policies or prefetching

Keywords: cache simulator, memory hierarchy, associativity, replacement policy, inclusion, write-back, write-allocate
