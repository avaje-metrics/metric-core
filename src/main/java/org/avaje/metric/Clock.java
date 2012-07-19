package org.avaje.metric;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * An abstraction for how time passes.
 */
public abstract class Clock {

  /**
   * Returns the current time tick.
   * 
   * @return time tick in nanoseconds
   */
  public abstract long getTickNanos();

  /**
   * Returns the current time in milliseconds.
   * 
   * @return time in milliseconds
   */
  public long getTimeMillis() {
    return System.currentTimeMillis();
  }

  private static final Clock DEFAULT = new UserTimeClock();

  /**
   * The default clock to use.
   * 
   * @return the default {@link Clock} instance
   */
  public static Clock defaultClock() {
    return DEFAULT;
  }

  /**
   * A clock implementation which returns the current time in epoch nanoseconds.
   */
  public static class UserTimeClock extends Clock {
    @Override
    public long getTickNanos() {
      return System.nanoTime();
    }
  }

  /**
   * A clock implementation which returns the current thread's CPU time.
   */
  public static class CpuTimeClock extends Clock {
    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    @Override
    public long getTickNanos() {
      return THREAD_MX_BEAN.getCurrentThreadCpuTime();
    }
  }
}
