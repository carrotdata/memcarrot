[Quick start guide](https://github.com/carrotdata/memcarrot/wiki/Quick-start)  🥕🥕 [Forum & Discussions](https://github.com/carrotdata/memcarrot/discussions) 🥕🥕 [Blog](https://medium.com/carrotdata)

# Memcarrot - The Memcached 2.0 You've Been Waiting For

Memcarrot is a caching server fully compatible with the Memcached protocol, offering superior memory utilization (with memory overhead as low as 6 bytes per object), real-time data compression for keys and values, efficient handling of expired items, zero internal and external memory fragmentation, intelligent data tiering, and complete persistence support.

## Quick Overview of Features

- **SmartReal Compression** - Server-side, real-time data compression (both keys and values) with a compression algorithm that continuously adapts to the current workload. It is up to 5.4x more memory efficient than using client-side compression with `Memcached`. We have conducted a series of memory benchmark tests, which you can find in our [membench](https://github.com/carrotdata/membench) repository.
- **True Data Tiering** - With only 11 bytes of in-memory metadata overhead (including expiration time), virtually any object can be efficiently stored on SSD. This makes it the most memory-efficient disk-based cache available today. Unlike `Memcached extstore` or `Redis` Enterprise, `Memcarrot` does not keep keys in RAM, significantly reducing RAM requirements to support the data index.
- **Multitiering** - Supports hybrid (RAM -> SSD) and tandem (RAM -> compressed RAM) configurations. 
- **Highly Configurable** - Users can customize cache admission policies (important for SSD), promotion policies (from victim cache back to the parent cache), eviction policies, and throughput controllers. Additional customizable components include memory index formats, internal GC recycling selectors, data writers, and data readers.
- **AI/ML Ready** - Custom cache admission and eviction policies can leverage sophisticated machine learning models tailored to specific workloads.
- **CacheGuard Protected** - Combines a cache admission policy with a scan-resistant cache eviction algorithm, significantly reducing SSD wear and increasing longevity.
- **Low SSD Write Amplification (DWA) and Cache Level Write Amplification (CLWA)** - With estimates of DLWA = 1.1 at 75% SSD usage, and 1.8 at 100%, even nearly full SSDs do not incur significant DLWA.
- **Low RAM Overhead for Cached Items** - Overhead ranges from 8 bytes per item for both RAM and SSD, including expiration support. The overhead depends on the index format used. Several index formats, both with and without expiration support, are provided out of the box.
- **Low Meta Overhead in RAM** - For example, managing 10M data items in `Memcarrot` requires less than 1MB of Java heap and less than 100MB of Java off-heap memory for metadata. To keep index data for 5B objects in memory 55GB of RAM is required, this is roughly 11 bytes per object.
- **Fragmentation and slab calcification free** storage engine. No more periodic server restarts are reqired to fight these problems (as for Redis amd Memcached)
- **Multiple Eviction Algorithms** - Available out of the box, including Segmented LRU (default), LRU, and FIFO. Segmented LRU is a scan-resistant algorithm. Eviction policies are pluggable, allowing customers to implement their own.
- **Scalability** - Supports multiple terabytes of storage, up to 256TB, with only 11 bytes of RAM overhead per cached item for disk storage.
- **Efficient Expired Item Eviction** - Designed for applications requiring expiration support.
- **Warm Restart** - Allows cache data to survive a full server reboot. Data saving and loading are very fast, dependent only on available disk I/O throughput (GBs per second).
- **Memcached Support** - Currently supports the text protocol only, including all data commands like `cas`, `stats`, and `version`. There is no support for Memcached-specific server commands (as they are not needed). We are working on improving compatibility with `Memcached`, so stay tuned.
- **Carrot Cache Powered** - See [Carrot Cache](https://github.com/carrotdata/carrot-cache) for more information and additional features.

## Requirements

- Java 11+
- Supported platforms: Linux (amd64, aarch64) + glibc 2.31+, MacOS Sonoma(x86_64, aarch64)

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
- gcc

### Building Memcarrot from Source

- Follow the steps to build `Carrot Cache` locally: [Carrot Cache](https://github.com/carrotdata/carrot-cache)
- Clone and build `Memcarrot`:
  ```bash
  git clone https://github.com/carrotdata/memcarrot.git
  cd memcarrot
  mvn package -DskipTests
  ```
## Docker images

`Carrot Data` provides docker images in the Docker Hub repository for `amd64` and `arm64` platforms.

Pull `amd64` image:

```bash
docker pull carrotdata/memcarrot:latest-amd64
```

Pull `arm64` image:

```bash
docker pull carrotdata/memcarrot:latest-arm64
```

These images have the following default configurations:

```bash
# Memcarrot - specific
server.address=0.0.0.0
server.port=11211
workers.pool.size=2
tcp.buffer.size=8192

# Inherited from carrot cache
storage.size.max=4g
index.format.impl=com.carrotdata.cache.index.SubCompactBaseNoSizeWithExpireIndexFormat
recycling.selector.impl=com.onecache.core.controllers.MinAliveRecyclingSelector
tls.supported=true
save.on.shutdown=true
compression.enabled=true
vacuum.cleaner.interval=30
```

`Memcarrot` allows configuration properties to be overridden via environment variables, enabling easy configuration of Docker containers.
Example: start `Memcarrot` docker container with maximum cache size `16GB` and worker pool size - 8:

```bash
docker network create --driver bridge memcarrot_network
docker run --network memcarrot_network -d \
  --name "memcarrot" \
  -p 11211:11211 \
  -e workers.pool.size=8 \
  -e storage.size.max=16g \
  "carrotdata/memcarrot:latest-amd64"
```

Alternatively, you can provide an environment configuration file to the `docker run` command using the `--env-file` option:

```bash
docker network create --driver bridge memcarrot_network
docker run --network memcarrot_network -d \
  --name "memcarrot" \
  --env-file /path/to/env.list \
  -p 11211:11211 \
  "carrotdata/memcarrot:latest-amd64"
```

This method allows you to easily pass multiple environment variables to your container in one go, without manually specifying each variable. The environment file should contain key-value pairs in the format `KEY=VALUE`, with each variable on a new line. For example:

```bash
workers.pool.size=8
storage.size.max=16g
```

Using an environment configuration file is especially useful when dealing with complex setups that require numerous configuration options. It helps keep your `docker run` commands cleaner and more manageable, and it ensures that your environment variables can be versioned and shared across different environments (e.g., development, staging, production).

## Memcarrot configuration

All server configuration parameters are contained in `conf/memcarrot.cfg`. This is the default location of this file. 

**Example 1:** In-memory cache, maximum size = 16GB, data segment size = 16MB, compression enabled, cache name = "L1", port = 123, host = 0.0.0.0

```bash
# Server port
server.port=123
# Server host
server.address=0.0.0.0

cache.names=L1
cache.types=memory
L1.cache.compression.enabled=true
L1.data.segment.size=16777216
L1.storage.size.max=17179869184
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
L2.data.segment.size=67108864
L2.storage.size.max=107374182400
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
L1.data.segment.size=16777216
L1.storage.size.max=17179869184

L2.cache.compression.enabled=true
L2.data.segment.size=67108864
L2.storage.size.max=107374182400
```

**Example 4:** Tandem cache (memory -> compressed memory)

```bash
# Server port
server.port=123
# Server host
server.address=0.0.0.0

cache.names=L1,L2
cache.types=memory,memory

L1.data.segment.size=16777216
L1.storage.size.max=17179869184

L2.cache.compression.enabled=true
L2.data.segment.size=67108864
L2.storage.size.max=107374182400
```

---

## Benchmarks

Hardware: MacOS Studio, 64GB RAM, SSD: 1TB
OS: Ubuntu Linux 22.04 (Parallels VM) 

### Memory usage 

See [```membench```](https://github.com/carrotdata/membench) for more benchmark details.

Picture 1. Memory usage in GB.
![Memory usage in GB](https://github.com/carrotdata/membench/blob/main/assets/memory.png)

### Performance

#### Membench load throughput

Picture 2. Load throughput in Kops
![Load throughput in Kops](https://github.com/carrotdata/membench/blob/main/assets/perf.png)


#### Memtier benchmark

See [```memtier_benchmark```](https://github.com/RedisLabs/memtier_benchmark) 

**Memcarrot 0.15**

![Memcarrot results](/assets/memcarrot.png)

**Memcached 1.6.29**

![Memcached results](/assets/memcached.png)

Contact: Vladimir Rodionov vlad@trycarrots.io. 
Copyright (c) Carrot Data, Inc., 2024

