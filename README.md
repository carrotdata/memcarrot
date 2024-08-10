
# Memcarrot - The Memcached 2.0 You've Been Waiting For

Memcarrot is a caching server fully compatible with the Memcached protocol, offering superior memory utilization (with memory overhead as low as 6 bytes per object), real-time data compression for keys and values, efficient handling of expired items, zero internal and external memory fragmentation, intelligent data tiering, and complete persistence support.

## Quick Overview of Features

- **SmartReal Compression** - Server-side, real-time data compression (both keys and values) with a compression algorithm that continuously adapts to the current workload. It is up to 5.4x more memory efficient than using client-side compression with `Memcached`. We have conducted a series of memory benchmark tests, which you can find in our [membench](https://github.com/carrotdata/membench) repository.
- **True Data Tiering** - With only 11 bytes of in-memory metadata overhead (including expiration time), virtually any object can be efficiently stored on SSD. This makes it the most memory-efficient disk-based cache available today. Unlike `Memcached extstore` or `Redis` Enterprise, `Memcarrot` does not keep keys in RAM, significantly reducing RAM requirements to support the data index.
- **Multitiering** - Supports hybrid (RAM -> SSD) and tandem (RAM -> compressed RAM) configurations. You can even build 3-level (or 4-, 5-, etc.) caches.
- **Highly Configurable** - Users can customize cache admission policies (important for SSD), promotion policies (from victim cache back to the parent cache), eviction policies, and throughput controllers. Additional customizable components include memory index formats, internal GC recycling selectors, data writers, and data readers.
- **AI/ML Ready** - Custom cache admission and eviction policies can leverage sophisticated machine learning models tailored to specific workloads.
- **CacheGuard Protected** - Combines a cache admission policy with a scan-resistant cache eviction algorithm, significantly reducing SSD wear and increasing longevity.
- **Low SSD Write Amplification (DWA) and Cache Level Write Amplification (CLWA)** - With estimates of DLWA = 1.1 at 75% SSD usage, and 1.8 at 100%, even nearly full SSDs do not incur significant DLWA.
- **Low RAM Overhead for Cached Items** - Overhead ranges from 8 bytes per item for both RAM and SSD, including expiration support. The overhead depends on the index format used. Several index formats, both with and without expiration support, are provided out of the box.
- **Low Meta Overhead in RAM** - For example, managing 1M data items in `Memcarrot` requires less than 1MB of Java heap and less than 10MB of Java off-heap memory for metadata.
- **Multiple Eviction Algorithms** - Available out of the box, including Segmented LRU (default), LRU, and FIFO. Segmented LRU is a scan-resistant algorithm. Eviction policies are pluggable, allowing customers to implement their own.
- **Scalability** - Supports multiple terabytes of storage, up to 256TB, with only 11 bytes of RAM overhead per cached item for disk storage.
- **Efficient Expired Item Eviction** - Designed for applications requiring expiration support.
- **Warm Restart** - Allows cache data to survive a full server reboot. Data saving and loading are very fast, dependent only on available disk I/O throughput (GBs per second).
- **Memcached Support** - Currently supports the text protocol only, including all data commands like `cas`, `stats`, and `version`. There is no support for Memcached-specific server commands (as they are not needed). We are working on improving compatibility with `Memcached`, so stay tuned.
- **Carrot Cache Powered** - See [Carrot Cache](https://github.com/carrotdata/carrot-cache) for more information and additional features.

## Requirements

- Java 11+
- Supported platforms: Linux (amd64, aarch64), macOS (x86_64, aarch64)

## Installation

- Download the deployment bundle from the [Releases](https://github.com/carrotdata/memcarrot/releases) page.
- Unpack it into a directory of your choice.

## Operation

- To start the server:
  ```bash
   ./bin/memcarrot.sh start
  ```
- To stop the server:
  ```bash
  ./bin/memcarrot.sh stop
  ```

## How to Build from Source

If your platform is not supported yet, you can build binaries from the source code.

### Requirements

- Java 11+
- Git
- Maven 3.x

### Building Memcarrot from Source

- Follow the steps to build `Carrot Cache` locally: [Carrot Cache](https://github.com/carrotdata/carrot-cache)
- Clone and build `Memcarrot`:
  ```bash
  git clone https://github.com/carrotdata/memcarrot.git
  cd memcarrot
  mvn package -DskipTests
  ```

## Server Configuration

All server configuration parameters are contained in `conf/memcarrot.cfg`. This is the default location of this file. You can provide a custom location for the configuration file:

```bash
bin/memcarrot.sh config-full-path-name start
```

Don't forget to add this path when you run the stop command:

```bash
bin/memcarrot.sh config-full-path-name stop
```

### Configuring the Server

**Example 1:** In-memory cache, maximum size = 16GB, data segment size = 16MB, compression enabled, cache name = "L1", port = 123, host = 0.0.0.0

```bash
# Server port
server.port=123
# Server host
server.address=0.0.0.0

cache.names=L1
cache.types=memory
L1.cache.compression.enabled=true
L1.data.segment.size=16_777_216
L1.storage.size.max=17_179_869_184
```

**Example 2:** Disk cache, maximum size = 100GB, data segment size = 64MB, compression enabled, cache name = "L2", port = 123, host = 0.0.0.0

```bash
# Server port
server.port=123
# Server host
server.address=0.0.0.0

cache.names=L2
cache.types=file
L2.cache.compression.enabled=true
L2.data.segment.size=67,108,864
L2.storage.size.max=107,374,182,400
```

**Example 3:** Hybrid cache (memory -> disk), both with compression enabled

```bash
# Server port
server.port=123
# Server host
server.address=0.0.0.0

cache.names=L1,L2
cache.types=memory,file

L1.cache.compression.enabled=true
L1.data.segment.size=16_777_216
L1.storage.size.max=17_179_869_184

L2.cache.compression.enabled=true
L2.data.segment.size=67,108,864
L2.storage.size.max=107,374,182,400
```

**Example 4:** Tandem cache (memory -> compressed memory)

```bash
# Server port
server.port=123
# Server host
server.address=0.0.0.0

cache.names=L1,L2
cache.types=memory,memory

L1.data.segment.size=16_777_216
L1.storage.size.max=17_179_869_184

L2.cache.compression.enabled=true
L2.data.segment.size=67,108,864
L2.storage.size.max=107,374,182,400
```

---

## Benchmarks

See [membench](https://github.com/carrotdata/membench) for benchmark description.

Table 1. RAM Usage and load throughput. Each result cell contains three numbers: number of objects loaded, server memory usage at the end of a benchmark run and average load throughput in records per second

| Server | airbnb | amazon_product_reviews | arxiv | dblp | github | ohio | reddit | spotify | twitter | twitter_sentiments |
| :---: | :---: | :---: | :---: | :---: | :--: | :---: | :---: | :---: | :---: | :---: |
| Memcarrot 0.11 | 20M, 8.38GB, 356K | 40M, 8.9GB, 535K | 20M, 10.8GB, 302K | 50M, 6.2GB, 680K | 40M, 3.0GB, 734K | 100M, 4.16GB, 805K | 10M, 3.33GB, 368K | 40M, 4GB, 655K | 10M, 5.4GB, 293K | 50M, 5.11GB, 755K |
| memcached 1.6.29 | 20M, 19.4GB, 518K | 40M, 18.23GB, 576K | 20M, 20.44GB, 521K | 50M, 18.7GB, 670K | 40M, 14.2GB, 582K | 100M, 18.9GB, 644K | 10M, 13.0GB, 419K | 40M, 22.0GB, 556K | 10M, 11.7GB, 426K | 50M, 16.4GB, 726K |


Picture 1. Memory usage in GB.
![Memory usage in GB](https://github.com/carrotdata/membench/blob/main/assets/memory.png)

Picture 2. Load throughput in Kops
![Load throughput in Kops](https://github.com/carrotdata/membench/blob/main/assets/perf.png)

Contact: Vladimir Rodionov vlad@trycarrots.io. 
Copyright (c) Carrot Data, Inc., 2024

