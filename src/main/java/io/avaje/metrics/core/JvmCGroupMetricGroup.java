package io.avaje.metrics.core;

import io.avaje.metrics.GaugeLong;
import io.avaje.metrics.GaugeLongMetric;
import io.avaje.metrics.Metric;
import io.avaje.metrics.MetricName;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static java.math.BigDecimal.valueOf;

class JvmCGroupMetricGroup {

  private final List<Metric> metrics = new ArrayList<>();

  /**
   * Return the list of OS process memory metrics.
   */
  static List<Metric> createGauges() {
    return new JvmCGroupMetricGroup().metrics();
  }

  private void add(Metric metric) {
    if (metric != null) {
      metrics.add(metric);
    }
  }

  private List<Metric> metrics() {

    FileLines cpu = new FileLines("/sys/fs/cgroup/cpu,cpuacct/cpuacct.usage");
    if (cpu.exists()) {
      add(createCGroupCpuUsage(cpu));
    }

    FileLines cpuStat = new FileLines("/sys/fs/cgroup/cpu,cpuacct/cpu.stat");
    if (cpuStat.exists()) {
      add(createCGroupCpuThrottle(cpuStat));
    }

    FileLines cpuShares = new FileLines("/sys/fs/cgroup/cpu,cpuacct/cpu.shares");
    if (cpuStat.exists()) {
      add(createCGroupCpuRequests(cpuShares));
    }

    FileLines cpuQuota = new FileLines("/sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us");
    FileLines period = new FileLines("/sys/fs/cgroup/cpu,cpuacct/cpu.cfs_period_us");
    if (cpuQuota.exists() && period.exists()) {
      add(createCGroupCpuLimit(cpuQuota, period));
    }

    return metrics;
  }


  GaugeLongMetric createCGroupCpuLimit(FileLines cpuQuota, FileLines period) {

    final long cpuQuotaVal = cpuQuota.single();
    long quotaPeriod = period.single();

    if (cpuQuotaVal > 0 && quotaPeriod > 0) {
      final long limit = convertQuotaToLimits(cpuQuotaVal, quotaPeriod);
      return new DefaultGaugeLongMetric(name("jvm.cgroup.cpu_limit"), new FixedGauge(limit));
    }

    return null;
  }

  GaugeLongMetric createCGroupCpuRequests(FileLines cpuShares) {

    final long requests = convertSharesToRequests(cpuShares.single());
    return new DefaultGaugeLongMetric(name("jvm.cgroup.cpu_requests"), new FixedGauge(requests));
  }

  long convertQuotaToLimits(long cpuQuotaVal, long quotaPeriod) {
    return valueOf(cpuQuotaVal)
      .multiply(valueOf(1000)) // to micro cores
      .divide(valueOf(quotaPeriod), RoundingMode.HALF_UP)
      .longValue();
  }

  /**
   * Convert docker cpu shares to K8s micro cores.
   */
  long convertSharesToRequests(long shares) {
    return valueOf(shares)
      .multiply(valueOf(1000))
      .divide(valueOf(1024), RoundingMode.HALF_UP)
      .setScale(-1, RoundingMode.HALF_UP)
      .longValue();
  }

  private GaugeLongMetric createCGroupCpuUsage(FileLines cpu) {
    return incrementing(name("jvm.cgroup.cpu_usage_micros"), new CpuUsageMicros(cpu));
  }

  private GaugeLongMetric createCGroupCpuThrottle(FileLines cpuStat) {
    return incrementing(name("jvm.cgroup.cpu_throttle_micros"), new CpuThrottleMicros(cpuStat));
  }

  private GaugeLongMetric incrementing(MetricName name, GaugeLong gauge) {
    return DefaultGaugeLongMetric.incrementing(name, gauge);
  }

  private MetricName name(String s) {
    return new DefaultMetricName(s);
  }

  static class CpuUsageMicros implements GaugeLong {

    private final FileLines source;

    CpuUsageMicros(FileLines source) {
      this.source = source;
    }

    @Override
    public long getValue() {
      return source.singleMicros();
    }
  }

  static class CpuThrottleMicros implements GaugeLong {

    private final FileLines source;

    CpuThrottleMicros(FileLines source) {
      this.source = source;
    }

    @Override
    public long getValue() {
      return parse(source.readLines());
    }

    private long parse(List<String> lines) {
      for (String line : lines) {
        if (line.startsWith("throttled_time")) {
          // convert from nanos to micros
          return Long.parseLong(line.substring(15)) / 1000;
        }
      }
      return 0;
    }
  }

  static class FixedGauge implements GaugeLong {

    private final long value;

    FixedGauge(long value) {
      this.value = value;
    }

    @Override
    public long getValue() {
      return value;
    }
  }
}