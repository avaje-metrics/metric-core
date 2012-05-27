package org.avaje.metric.core;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.avaje.metric.Clock;
import org.avaje.metric.EventMetric;
import org.avaje.metric.Metric;
import org.avaje.metric.MetricName;
import org.avaje.metric.TimedMetric;

public class DefaultMetricManager {

  private final String monitor = new String();

  private final ConcurrentHashMap<String, Metric> concMetricMap = new ConcurrentHashMap<String, Metric>();

  private final Timer timer = new Timer("MetricManager", true);

  private final JmxMetricRegister jmxRegistry = new JmxMetricRegister();

  private final MetricFactory timedMetricFactory = new TimedMetricFactory();
  private final MetricFactory eventMetricFactory = new EventMetricFactory();
  
  public DefaultMetricManager() {
    timer.scheduleAtFixedRate(new UpdateStatisticsTask(), 5 * 1000, 2 * 1000);
  }

  private class UpdateStatisticsTask extends TimerTask {

    @Override
    public void run() {
      updateStatistics();
    }
  }

  public void updateStatistics() {
    Collection<Metric> allMetrics = getAllMetrics();
    for (Metric metric : allMetrics) {
      metric.updateStatistics();
    }
  }

  public TimedMetric getTimedMetric(MetricName name, TimeUnit rateUnit, Clock clock) {
    return (TimedMetric) getMetric(name, rateUnit, clock, timedMetricFactory);
  }
  
  public EventMetric getEventMetric(MetricName name, TimeUnit rateUnit) {
    return (EventMetric) getMetric(name, rateUnit, null, eventMetricFactory);
  }
  

  private Metric getMetric(MetricName name, TimeUnit rateUnit, Clock clock, MetricFactory factory) {

    String cacheKey = name.getMBeanName();
    // try lock free get first
    Metric metric = concMetricMap.get(cacheKey);
    if (metric == null) {
      synchronized (monitor) {
        // use synchronised block
        metric = concMetricMap.get(cacheKey);
        if (metric == null) {
          metric = factory.createMetric(name, rateUnit, clock, jmxRegistry);
          concMetricMap.put(cacheKey, metric);
        }
      }
    }
    return metric;
  }

  public void clear() {
    synchronized (monitor) {
      Collection<Metric> values = concMetricMap.values();
      for (Metric metric : values) {
        jmxRegistry.unregister(metric.getName().getMBeanObjectName());
        if (metric instanceof TimedMetric) {
          ObjectName errorMBeanName = ((TimedMetric) metric).getErrorMBeanName();
          jmxRegistry.unregister(errorMBeanName);
        }

      }
      concMetricMap.clear();
    }
  }

  public Collection<Metric> getAllMetrics() {
    synchronized (monitor) {
      return concMetricMap.values();
    }
  }

}