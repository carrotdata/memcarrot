# Memcarrot

Memcarrot is a high-performance in-memory data store designed for scalability and reliability. This documentation
provides instructions on how to deploy Memcarrot using Docker in various configurations, including standalone mode, with
Prometheus and Grafana as external services, and combined with Prometheus and Grafana for comprehensive monitoring.

## Table of Contents

- [Getting Started](#getting-started)
    - [Running Memcarrot Standalone](#running-memcarrot-standalone)
        - [Access Memcarrot](#access-memcarrot)
    - [Running Prometheus and Grafana as External Services](#running-prometheus-and-grafana-as-external-services)
        - [Start Prometheus and Grafana](#start-prometheus-and-grafana)
        - [Access the Services](#access-the-services)
    - [Running Memcarrot with Prometheus and Grafana Together](#running-memcarrot-with-prometheus-and-grafana-together)
        - [Start All Services](#start-all-services)
        - [Access the Services](#access-the-services-1)
- [Configuration](#configuration)
    - [Enabling JMX Exporter](#enabling-jmx-exporter)
    - [Configuring via Volumes](#configuring-via-volumes)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Getting Started

### Running Memcarrot Standalone

To run Memcarrot independently using Docker Compose, use the default `docker-compose.yml` provided.

## Getting Started

### Running Memcarrot Standalone

To run Memcarrot independently using Docker Compose, use the default `docker-compose.yml` provided.

#### Start Memcarrot:

```bash
docker-compose up -d
```

#### Access Memcarrot:

*Application: http://localhost:11211*
*Memcached Exporter Metrics: http://localhost:9191/metrics*

### Running Prometheus and Grafana as External Services

To set up Prometheus and Grafana alongside Memcarrot as external services, use the `docker-grafana-compose.yml`
provided.

#### Start Prometheus and Grafana:

```bash
docker-compose -f docker-grafana-compose.yml up -d
```

#### Access the Services:

- *Prometheus: http://localhost:9090/metrics*
- *Grafana: http://localhost:3000/login*

### Running Memcarrot with Prometheus and Grafana Together

To deploy Memcarrot alongside Prometheus and Grafana in a single Docker Compose setup, use the
`docker-memcarrot-grafana-compose.yml` provided.

#### Start All Services:

```bash
docker-compose -f docker-memcarrot-grafana-compose.yml up -d
```

#### Access the Services:

- *Memcarrot: http://localhost:11211*
- *Prometheus: http://localhost:9090/metrics*
- *Grafana: http://localhost:3000/login*

## Configuration

### Enabling JMX Exporter

Memcarrot can be configured to enable or disable the JMX exporter for monitoring purposes. This is controlled via the
JMX_EXPORTER_ENABLED environment variable in the setenv.sh scripts.
To enable the JMX exporter, set the JMX_EXPORTER_ENABLED environment variable to true in your Docker Compose file or
environment configuration.

- *Standalone (setenv.sh for on-premises):*
export JMX_EXPORTER_ENABLED=true

- *Docker (setenv_4docker.sh for Docker image):*
export JMX_EXPORTER_ENABLED=true

### Configuring via Volumes

The Docker image for Memcarrot uses volumes to map configuration and data files from the host to the container. This
allows for easy customization and persistence of data.

volumes:

- ./bin/setenv_4docker.sh:/users/carrotdata/memcarrot/bin/setenv.sh
- ./conf/memcarrot.cfg:/users/carrotdata/memcarrot/conf/memcarrot.cfg
- ./jmx/jmxconfig.yml:/users/carrotdata/memcarrot/conf/jmxconfig.yml
- ./data:/users/carrotdata/memcarrot/data
- ./logs:/users/carrotdata/memcarrot/logs


- `setenv_4docker.sh`: Configure environment variables.
- `memcarrot.cfg`: Main configuration file for Memcarrot.
- `jmxconfig.yml`: Configuration for JMX exporter.
- `data`: Directory for storing application data.
- `logs`: Directory for storing logs.

Ensure these files and directories exist on your host system and are correctly referenced in the respective
docker-compose.yml files.

## Troubleshooting

- Container Not Starting: Check Docker logs using docker logs <container_name> to identify and resolve startup issues.
- Port Conflicts: Ensure the ports specified in docker-compose.yml are not being used by other applications.
- Configuration Errors: Validate your configuration files for syntax errors and ensure all required settings are
  provided.
- Network Issues: Verify that Docker networks are correctly configured and that containers can communicate with each
  other.

