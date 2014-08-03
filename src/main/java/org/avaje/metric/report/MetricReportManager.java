package org.avaje.metric.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.avaje.metric.Metric;
import org.avaje.metric.MetricManager;

/**
 * Writes the collected metrics to registered reporters.
 * <p>
 * Typically you configure the frequency in seconds in which statistics are collected and reported
 * as well as a base directory where the metric files go. By default the base directory will be read
 * from a system property 'metric.directory' and otherwise defaults to the current directory.
 * </p>
 */
public class MetricReportManager {

  private static final Logger logger = Logger.getLogger(MetricReportManager.class.getName());

  private static final int EIGHT_HOURS = 60 * 60 * 8;

  protected final Timer timer;

  protected final int freqInSeconds;

  protected final MetricReporter localReporter;

  protected final MetricReporter remoteReporter;

  /**
   * Create specifying the reporting frequency and a reporter.
   * <p>
   * This will create a Timer to execute the reporting periodically.
   */
  public MetricReportManager(int freqInSeconds, MetricReporter reporter) {
    this(new Timer("MetricReporter", true), freqInSeconds, reporter, null);
  }

  /**
   * Create specifying a second reporter.
   * <p>
   * Having 2 reporters can be useful if you want to store to a local file system and report the
   * metrics to a central repository.
   */
  public MetricReportManager(int freqInSeconds, MetricReporter localReporter, MetricReporter remoteReporter) {
    this(new Timer("MetricReporter", true), freqInSeconds, localReporter, remoteReporter);
  }

  /**
   * Create specifying the Timer to use.
   */
  public MetricReportManager(Timer timer, int freqInSeconds, MetricReporter reporter) {
    this(timer, freqInSeconds, reporter, null);
  }

  /**
   * Create specifying a Timer, reporting frequency and 2 reporters.
   * <p>
   * Having 2 reporters can be useful if you want to store to a local file system and report the
   * metrics to a central repository.
   */
  public MetricReportManager(Timer timer, int freqInSeconds, MetricReporter localReporter, MetricReporter remoteReporter) {

    this.timer = timer;
    this.localReporter = localReporter;
    this.remoteReporter = remoteReporter;
    this.freqInSeconds = freqInSeconds;
    long freqMillis = freqInSeconds * 1000;

    if (freqMillis > 0) {
      // Create the Timer and schedule the WriteTask
      this.timer.scheduleAtFixedRate(new WriteTask(), freqMillis, freqMillis);
    }

  }

  protected class WriteTask extends TimerTask {

    int cleanupCounter;

    public void run() {
      try {
        cleanupCounter++;
        reportMetrics();

        if (cleanupCounter * freqInSeconds > EIGHT_HOURS) {
          // cleanup old metric files about every 8 hours
          cleanupCounter = 0;
          periodicCleanUp();
        }

      } catch (IOException e) {
        logger.log(Level.SEVERE, "", e);
      }
    }
  }

  /**
   * Perform periodic (defaults to every 8 hours) cleanup.
   * <p>
   * This is used by file reporters to limit the number of metrics files held.
   */
  protected void periodicCleanUp() {
    if (localReporter != null) {
      localReporter.cleanup();
    }
    if (remoteReporter != null) {
      remoteReporter.cleanup();
    }
  }

  /**
   * Report all the metrics.
   * <p>
   * This typically means appending the metrics to a file or sending over a network.
   */
  protected void reportMetrics() throws IOException {

    List<Metric> metrics = collectMetrics();

    report(metrics, localReporter);
    report(metrics, remoteReporter);
  }

  /**
   * Collect all the non-empty metrics and return them for reporting.
   */
  protected List<Metric> collectMetrics() {
    List<Metric> metrics = sort(MetricManager.getJvmMetrics());
    List<Metric> otherMetrics = sort(MetricManager.collectNonEmptyMetrics());
    metrics.addAll(otherMetrics);
    return metrics;
  }

  /**
   * Sort the metrics into name order.
   */
  protected List<Metric> sort(Collection<Metric> metrics) {
    ArrayList<Metric> ar = new ArrayList<Metric>(metrics);
    Collections.sort(ar, new NameComp());
    return ar;
  }

  /**
   * Visit the metrics sorted by name.
   */
  protected void report(List<Metric> allMetrics, MetricReporter reporter) {

    if (reporter != null) {
      try {
        reporter.report(allMetrics);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Error trying to report metrics", e);
      }
    }
  }

  /**
   * Compare Metrics by name for sorting purposes.
   */
  protected static class NameComp implements Comparator<Metric> {

    @Override
    public int compare(Metric o1, Metric o2) {
      return o1.getName().compareTo(o2.getName());
    }

  }

}
