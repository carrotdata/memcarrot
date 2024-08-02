package com.carrotdata.memcarrot;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class MemcarrotConfYaml {
  private ServerConfig server;
  private WorkerConfig workers;
  private KVConfig kv;
  private TCPConfig tcp;
  private CacheProps cacheProps;
  private VictimConfig victim;
  private String rootDirPath;
  private long dataSegmentSize;
  private ScavengerConfig scavenger;
  private int popularityNumberRanks;
  private ActiveConfig active;
  private StorageConfig storage;
  private EvictionConfig eviction;
  private AdmissionConfig admission;
  private PromotionConfig promotion;
  private RandomConfig random;
  private long writeAvgRateLimit;
  private boolean sparseFilesSupport;
  private IndexConfig index;
  private ThroughputConfig throughput;
  private String promotionControllerImpl;
  private String recyclingSelectorImpl;
  private String dataWriterImpl;
  private MemoryConfig memory;
  private FileConfig file;
  private int blockWriterBlockSize;
  private ExpireConfig expire;
  private boolean evictionDisabled;
  private long spinWaitTimeNs;
  private String jmxMetricsDomainName;
  private boolean hybridInverseMode;
  private TLSConfig tls;
  private CompressionConfig compression;
  private boolean saveOnShutdown;
  private long estimatedAvgKvSize;
  private double proactiveExpirationFactor;
  private ObjectCacheConfig objectcache;
  private long vacuumCleanerInterval;

  public static MemcarrotConfYaml loadConfiguration() throws IOException {
    String fileName = System.getProperty("memcarrot.config", "memcarrot.yaml");
    LoaderOptions options = new LoaderOptions();
    Constructor constructor = new Constructor(MemcarrotConfYaml.class, options);
    Yaml yaml = new Yaml(constructor);

    try (InputStream inputStream = MemcarrotConfYaml.class.getClassLoader()
        .getResourceAsStream(fileName)) {
      if (inputStream == null) {
        throw new IOException("File not found: " + fileName);
      }
      return yaml.load(inputStream);
    }
  }

  public static void main(String[] args) {
    try {
      MemcarrotConfYaml config = MemcarrotConfYaml.loadConfiguration();
      // Print some properties to verify loading
      System.out.println("Server port: " + config.getServer().getPort());
      System.out.println("Server address: " + config.getServer().getAddress());
      System.out.println("Cache names: " + config.getCacheProps().getNames());
      System.out.println("Cache values: " + config.getCacheProps().getValues());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Data
  public static class ServerConfig {
    private int port;
    private String address;
  }

  @Data
  public static class WorkerConfig {
    private int poolSize;
  }

  @Data
  public static class KVConfig {
    private int sizeMax;
  }

  @Data
  public static class TCPConfig {
    private int bufferSize;
  }

  @Data
  public static class CacheProps {
    private List<String> names;
    private Map<String, CacheConfig> values;

    @Data
    public static class CacheConfig {
      private String types;
      private long storageSizeMax;
      private int waitPutMaxMs;
      private long maxKvSize;
    }
  }

  @Data
  public static class VictimConfig {
    private String name;
    private boolean promotionOnHit;
    private double promotionThreshold;
  }

  @Data
  public static class ScavengerConfig {
    private double ratioStart;
    private double ratioStop;
    private double dumpEntryBelowMin;
    private double dumpEntryBelowMax;
    private double scavengerDumpEntryBelowStep;
    private int numberThreads;
    private int runIntervalSec;
  }

  @Data
  public static class ActiveConfig {
    private double datasetRatioMin;
  }

  @Data
  public static class StorageConfig {
    private int ioPoolSize;
  }

  @Data
  public static class EvictionConfig {
    private int slruNumberSegments;
    private int slruInsertPoint;
    private String policyImpl;
  }

  @Data
  public static class AdmissionConfig {
    private double queueSizeRatioStart;
    private QueueConfig queue;
    private String controllerImpl;

    @Data
    public static class QueueConfig {
      private double sizeRatioMin;
      private double sizeRatioMax;
    }
  }

  @Data
  public static class PromotionConfig {
    private QueueConfig queue;

    @Data
    public static class QueueConfig {
      private double sizeRatioStart;
      private double sizeRatioMin;
      private double sizeRatioMax;
    }
  }

  @Data
  public static class RandomConfig {
    private double promotionProbability;
    private double admissionRatioStart;
    private double admissionRatioStop;
  }

  @Data
  public static class IndexConfig {
    private int slotsPower;
    private DataConfig data;
    private FormatConfig format;

    @Data
    public static class DataConfig {
      private boolean embedded;
      private int embeddedSizeMax;
    }

    @Data
    public static class FormatConfig {
      private String impl;
      private String aqImpl;
    }
  }

  @Data
  public static class ThroughputConfig {
    private int checkIntervalSec;
    private double toleranceLimit;
    private int adjustmentSteps;
    private String controllerImpl;
  }

  @Data
  public static class TLSConfig {
    private boolean supported;
    private BufferConfig buffer;

    @Data
    public static class BufferConfig {
      private int sizeStart;
      private int sizeMax;
    }
  }

  @Data
  public static class CompressionConfig {
    private boolean enabled;
    private int blockSize;
    private int dictionarySize;
    private int level;
    private String codec;
    private boolean dictionaryEnabled;
    private boolean keysEnabled;
    private boolean dictionaryTrainingAsync;
  }

  @Data
  public static class ObjectCacheConfig {
    private BufferConfig buffer;

    @Data
    public static class BufferConfig {
      private int sizeStart;
      private int sizeMax;
    }
  }

  @Data
  public static class FileConfig {
    private String dataReaderImpl;
    private int prefetchBufferSize;
  }

  @Data
  public static class MemoryConfig {
    private String dataReaderImpl;
    private int bufferPoolSizeMax;
  }

  @Data
  public static class ExpireConfig {
    private String supportImpl;
    private int binValueStart;
    private int multiplierValue;
  }
}
