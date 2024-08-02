package com.carrotdata.memcarrot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
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

  public static void main(String[] args) {
    try {
      var config = MemcarrotConfYaml.loadConfigProfile("conf/memcarrot-prod.yaml");
      // Print some properties to verify loading
      System.out.println("Server port: " + config.getServer().getPort());
      System.out.println("Server address: " + config.getServer().getAddress());
      System.out.println("Cache names: " + config.getCacheProps().getNames());
      System.out.println("Cache values: " + config.getCacheProps().getValues());

      System.out.println("Config json: " + config.toJson());
      System.out.println("Config yaml: " + config.toYaml());
    } catch (IOException e) {
      System.out.println("Error loading configuration" + e);
    }
  }

  public static MemcarrotConfYaml loadConfigProfile(String filePath) throws IOException {
    var defaultConfig = loadConfigDefault();
    if ("default".equals(filePath) || "".equals(filePath) || Objects.isNull(filePath)) {
      return defaultConfig;
    }

    if (!new File(filePath).exists()) {
      System.out.println("File not found: {}\n loaded default profile only" + filePath);
      return defaultConfig;
    }

    var options = new LoaderOptions();
    var constructor = new Constructor(MemcarrotConfYaml.class, options);
    var yaml = new Yaml(constructor);

    MemcarrotConfYaml config;
    try (InputStream inputStream = new FileInputStream(filePath)) {
      config = yaml.load(inputStream);
    }
    if (config == null) {
      return defaultConfig;
    }
    return mergeConfigurations(defaultConfig, config);
  }

  private static MemcarrotConfYaml loadConfigDefault() throws IOException {
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

  private static MemcarrotConfYaml mergeConfigurations(MemcarrotConfYaml baseConfig,
      MemcarrotConfYaml overrideConfig) {
    if (overrideConfig == null) {
      System.out.println("Override configuration is null, returning default configuration");
      return baseConfig;
    }

    try {
      for (Field field : MemcarrotConfYaml.class.getDeclaredFields()) {
        field.setAccessible(true);
        Object overrideValue = field.get(overrideConfig);
        if (overrideValue != null) {
          field.set(baseConfig, overrideValue);
        }
      }
    } catch (IllegalAccessException e) {
      System.out.println("Error merging configurations" + e);
    }
    return baseConfig;
  }

  public String toYaml() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    Representer representer = new Representer(options);
    Yaml yaml = new Yaml(representer, options);
    StringWriter writer = new StringWriter();
    yaml.dump(this, writer);
    return writer.toString();
  }

  public String toJson() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
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
