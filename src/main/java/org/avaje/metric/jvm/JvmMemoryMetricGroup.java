package org.avaje.metric.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.avaje.metric.GaugeDouble;
import org.avaje.metric.GaugeDoubleGroup;
import org.avaje.metric.GaugeDoubleMetric;
import org.avaje.metric.MetricName;
import org.avaje.metric.core.DefaultGaugeDoubleMetric;
import org.avaje.metric.core.DefaultGaugeDoubleGroup;
import org.avaje.metric.core.DefaultMetricName;

public final class JvmMemoryMetricGroup {

  /**
   * Helper interface for Heap and NonHeap MemorySource.
   */
  static interface MemoryUsageSource {
    MemoryUsage getUsage();
  }
  
  /**
   * Heap MemorySource.
   */
  static class HeapMemoryUsageSource implements MemoryUsageSource {
    final MemoryMXBean memoryMXBean;
    HeapMemoryUsageSource(MemoryMXBean memoryMXBean) {
      this.memoryMXBean = memoryMXBean;
    }
    public MemoryUsage getUsage() {
      return memoryMXBean.getHeapMemoryUsage();
    }
  }
  
  /**
   * NonHeap MemorySource.
   */
  static class NonHeapMemoryUsageSource implements MemoryUsageSource {
    final MemoryMXBean memoryMXBean;
    NonHeapMemoryUsageSource(MemoryMXBean memoryMXBean) {
      this.memoryMXBean = memoryMXBean;
    }
    public MemoryUsage getUsage() {
      return memoryMXBean.getNonHeapMemoryUsage();
    }
  }
  
  private static final String[] names = {"pct","init","used","committed","max"};
  
  private static final double MEGABYTES = 1024*1024;
  
  /**
   * Create the Heap Memory based GaugeMetricGroup.
   */
  public static GaugeDoubleGroup createHeapGroup() {
    
    DefaultMetricName heapName = DefaultMetricName.createBaseName("jvm","memory.heap");   
    HeapMemoryUsageSource source = new HeapMemoryUsageSource(ManagementFactory.getMemoryMXBean());
    return createGroup(heapName, source);
  }
 
  /**
   * Create the NonHeap Memory based GaugeDoubleMetricGroup.
   */
  public static GaugeDoubleGroup createNonHeapGroup() {
    DefaultMetricName nonHeapName = DefaultMetricName.createBaseName("jvm","memory.nonheap");
    NonHeapMemoryUsageSource source = new NonHeapMemoryUsageSource(ManagementFactory.getMemoryMXBean());
    return createGroup(nonHeapName, source);
  }

  private static GaugeDoubleGroup createGroup(DefaultMetricName baseName, MemoryUsageSource source) {
    GaugeDouble[] gauges = createGauges(source);
    GaugeDoubleMetric[] group = createGroup(baseName, gauges);
    return new DefaultGaugeDoubleGroup(baseName, group);
  }
  
  private static GaugeDoubleMetric[] createGroup(DefaultMetricName baseName,  GaugeDouble[] gauges) {
    GaugeDoubleMetric[] group = new GaugeDoubleMetric[gauges.length];
    for (int i = 0; i < gauges.length; i++) {
      group[i] = createGaugeMetric(baseName, names[i], gauges[i]);
    }
    return group;
  }
  
  private static GaugeDoubleMetric createGaugeMetric(DefaultMetricName baseName, String name, GaugeDouble gauge) {
    MetricName specificName = baseName.withName(name);
    return new DefaultGaugeDoubleMetric(specificName, gauge);
  }
  
 
  private static GaugeDouble[] createGauges(MemoryUsageSource source) {
    return new MemUsageGauages(source).getGauges();
  }
  
  static class MemUsageGauages {
    private final GaugeDouble[] gauges;
    
    private MemUsageGauages(MemoryUsageSource source) {
      this.gauges = createGauges(source);
    }
    
    public GaugeDouble[] getGauges() {
      return gauges;
    }
    
    private GaugeDouble[] createGauges(MemoryUsageSource source) {
      GaugeDouble[] ga = new GaugeDouble[5];
      ga[0] = new Pct(source);
      ga[1] = new Init(source);
      ga[2] = new Used(source);
      ga[3] = new Committed(source);
      ga[4] = new Max(source);
      return ga;
    }
  
    private abstract class Base {
      MemoryUsageSource source;
      Base(MemoryUsageSource source) {
        this.source = source;
      }
    }
    private class Init extends Base implements GaugeDouble {
      Init(MemoryUsageSource source) {
        super(source);
      }
      @Override
      public double getValue() {
        return source.getUsage().getInit() / MEGABYTES;
      }
    }
    
    private class Used extends Base implements GaugeDouble {
      Used(MemoryUsageSource source) {
        super(source);
      }
      @Override
      public double getValue() {
        return source.getUsage().getUsed() / MEGABYTES;
      }
    }
    
    private class Committed extends Base implements GaugeDouble {
      Committed(MemoryUsageSource source) {
        super(source);
      }
      @Override
      public double getValue() {
        return source.getUsage().getCommitted() / MEGABYTES;
      }
    }
    
    private class Max extends Base implements GaugeDouble {
      Max(MemoryUsageSource source) {
        super(source);
      }
      @Override
      public double getValue() {
        return source.getUsage().getMax() / MEGABYTES;
      }
    }

    private class Pct extends Base implements GaugeDouble {
      Pct(MemoryUsageSource source) {
        super(source);
      }
      @Override
      public double getValue() {
        MemoryUsage memoryUsage = source.getUsage();
        return 100* memoryUsage.getUsed() / memoryUsage.getMax() ;
      }
    }
  }
  
}
