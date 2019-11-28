package io.avaje.metrics.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CounterTest {

  @Test
  public void test() {

    Counter counter = new Counter(null);
    assertEquals(0, counter.getCount());
    assertTrue(counter.isEmpty());

    counter.increment();
    assertFalse(counter.isEmpty());
    assertEquals(1, counter.getCount());

    counter.increment();
    assertEquals(2, counter.getCount());
    counter.decrement();
    assertEquals(1, counter.getCount());
    counter.add(100);
    assertEquals(101, counter.getCount());
    counter.reset();
    assertTrue(counter.isEmpty());


    counter.add(101);
    assertEquals(101, counter.getCount());

    counter.reset();
    assertTrue(counter.isEmpty());
    assertEquals(0, counter.getCount());


    counter.add(101);
    assertEquals(101, counter.getCount());
    assertFalse(counter.isEmpty());
    assertEquals(101, counter.getCount());

  }
}