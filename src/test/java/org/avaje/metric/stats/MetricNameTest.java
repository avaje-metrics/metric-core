package org.avaje.metric.stats;

import javax.management.ObjectName;

import org.avaje.metric.MetricName;
import org.junit.Assert;
import org.junit.Test;

public class MetricNameTest {

  @Test
  public void testType() {
    
    MetricName name = new MetricName("log","t","error");
    
    Assert.assertNotNull(name);
    ObjectName mBeanObjectName = name.getMBeanObjectName();
    Assert.assertNotNull(mBeanObjectName);
    
  }
  
  @Test
  public void testParse() {
    
    MetricName name = MetricName.parse("org.test.Hello.rob");
    
    Assert.assertNotNull(name);
    Assert.assertEquals("org.test",name.getGroup());
    Assert.assertEquals("Hello",name.getType());
    Assert.assertEquals("rob",name.getName());
    
    MetricName name2 = MetricName.parse("test.Hello.rob");
    
    Assert.assertNotNull(name);
    Assert.assertEquals("test",name2.getGroup());
    Assert.assertEquals("Hello",name2.getType());
    Assert.assertEquals("rob",name2.getName());
    
    MetricName name3 = MetricName.parse("Hello.rob");
    
    Assert.assertNotNull(name);
    Assert.assertEquals("o",name3.getGroup());
    Assert.assertEquals("Hello",name3.getType());
    Assert.assertEquals("rob",name3.getName());
  }
}