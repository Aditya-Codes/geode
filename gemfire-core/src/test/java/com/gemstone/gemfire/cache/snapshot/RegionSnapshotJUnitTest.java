/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.cache.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.examples.snapshot.MyObject;
import com.examples.snapshot.MyPdxSerializer;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.snapshot.RegionGenerator.RegionType;
import com.gemstone.gemfire.cache.snapshot.RegionGenerator.SerializationType;
import com.gemstone.gemfire.cache.snapshot.SnapshotOptions.SnapshotFormat;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.cache.util.CacheWriterAdapter;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

@Category(IntegrationTest.class)
public class RegionSnapshotJUnitTest extends SnapshotTestCase {
  private File f;
  
  @Test
  public void testExportAndReadSnapshot() throws Exception {
    for (final RegionType rt : RegionType.values()) {
      for (final SerializationType st : SerializationType.values()) {
        String name = "test-" + rt.name() + "-" + st.name();
        Region<Integer, MyObject> region = rgen.createRegion(cache, ds.getName(), rt, name);
        final Map<Integer, MyObject> expected = createExpected(st);
        
        region.putAll(expected);
        region.getSnapshotService().save(f, SnapshotFormat.GEMFIRE);
        
        final Map<Integer, Object> read = new HashMap<Integer, Object>();
        SnapshotIterator<Integer, Object> iter = SnapshotReader.read(f);
        try {
          while (iter.hasNext()) {
            Entry<Integer, Object> entry = iter.next();
            read.put(entry.getKey(), entry.getValue());
          }
          assertEquals("Comparison failure for " + rt.name() + "/" + st.name(), expected, read);
        } finally {
          iter.close();
        }
      }
    }
  }
  
  @Test
  public void testExportAndImport() throws Exception {
    for (final RegionType rt : RegionType.values()) {
      for (final SerializationType st : SerializationType.values()) {
        String name = "test-" + rt.name() + "-" + st.name();
        Region<Integer, MyObject> region = rgen.createRegion(cache, ds.getName(), rt, name);
        final Map<Integer, MyObject> expected = createExpected(st);

        region.putAll(expected);
        region.getSnapshotService().save(f, SnapshotFormat.GEMFIRE);

        region.destroyRegion();
        region = rgen.createRegion(cache, ds.getName(), rt, name);
        
        region.getAttributesMutator().setCacheWriter(new CacheWriterAdapter<Integer, MyObject>() {
          @Override
          public void beforeCreate(EntryEvent<Integer, MyObject> event) {
            fail("CacheWriter invoked during import");
          }
        });
        
        final AtomicBoolean cltest = new AtomicBoolean(false);
        region.getAttributesMutator().addCacheListener(new CacheListenerAdapter<Integer, MyObject>() {
          @Override
          public void afterCreate(EntryEvent<Integer, MyObject> event) {
            cltest.set(true);
          }
        });
        
        region.getSnapshotService().load(f, SnapshotFormat.GEMFIRE);
        
        assertEquals("Comparison failure for " + rt.name() + "/" + st.name(), expected.entrySet(), region.entrySet());
        assertEquals("CacheListener invoked during import", false, cltest.get());
      }
    }
  }
  
  @Test
  public void testFilter() throws Exception {
    SnapshotFilter<Integer, MyObject> even = new SnapshotFilter<Integer, MyObject>() {
      @Override
      public boolean accept(Entry<Integer, MyObject> entry) {
        return entry.getKey() % 2 == 0;
      }
    };
    
    SnapshotFilter<Integer, MyObject> odd = new SnapshotFilter<Integer, MyObject>() {
      @Override
      public boolean accept(Entry<Integer, MyObject> entry) {
        return entry.getKey() % 2 == 1;
      }
    };

    for (final RegionType rt : RegionType.values()) {
      for (final SerializationType st : SerializationType.values()) {
        String name = "test-" + rt.name() + "-" + st.name();
        Region<Integer, MyObject> region = rgen.createRegion(cache, ds.getName(), rt, name);
        final Map<Integer, MyObject> expected = createExpected(st);

        region.putAll(expected);
        RegionSnapshotService<Integer, MyObject> rss = region.getSnapshotService();
        SnapshotOptions<Integer, MyObject> options = rss.createOptions().setFilter(even);
        rss.save(f, SnapshotFormat.GEMFIRE, options);

        region.destroyRegion();
        region = rgen.createRegion(cache, ds.getName(), rt, name);
        
        rss = region.getSnapshotService();
        options = rss.createOptions().setFilter(odd);
        rss.load(f, SnapshotFormat.GEMFIRE, options);

        assertEquals("Comparison failure for " + rt.name() + "/" + st.name(), 0, region.size());
      }
    }
  }
  
  @Test
  public void testFilterExportException() throws Exception {
    SnapshotFilter<Integer, MyObject> oops = new SnapshotFilter<Integer, MyObject>() {
      @Override
      public boolean accept(Entry<Integer, MyObject> entry) {
        throw new RuntimeException();
      }
    };
    
    for (final RegionType rt : RegionType.values()) {
      for (final SerializationType st : SerializationType.values()) {
        String name = "test-" + rt.name() + "-" + st.name();
        Region<Integer, MyObject> region = rgen.createRegion(cache, ds.getName(), rt, name);
        final Map<Integer, MyObject> expected = createExpected(st);

        region.putAll(expected);
        RegionSnapshotService<Integer, MyObject> rss = region.getSnapshotService();
        SnapshotOptions<Integer, MyObject> options = rss.createOptions().setFilter(oops);
        
        boolean caughtException = false;
        try {
          rss.save(f, SnapshotFormat.GEMFIRE, options);
        } catch (RuntimeException e) {
          caughtException = true;
        }
        assertTrue(caughtException);
        
        region.destroyRegion();
        region = rgen.createRegion(cache, ds.getName(), rt, name);
        
        rss = region.getSnapshotService();
        rss.load(f, SnapshotFormat.GEMFIRE, options);

        assertEquals("Comparison failure for " + rt.name() + "/" + st.name(), 0, region.size());
      }
    }
  }
  
  @Test
  public void testFilterImportException() throws Exception {
    SnapshotFilter<Integer, MyObject> oops = new SnapshotFilter<Integer, MyObject>() {
      @Override
      public boolean accept(Entry<Integer, MyObject> entry) {
        throw new RuntimeException();
      }
    };
    
    for (final RegionType rt : RegionType.values()) {
      for (final SerializationType st : SerializationType.values()) {
        String name = "test-" + rt.name() + "-" + st.name();
        Region<Integer, MyObject> region = rgen.createRegion(cache, ds.getName(), rt, name);
        final Map<Integer, MyObject> expected = createExpected(st);

        region.putAll(expected);
        RegionSnapshotService<Integer, MyObject> rss = region.getSnapshotService();
        rss.save(f, SnapshotFormat.GEMFIRE);

        region.destroyRegion();
        region = rgen.createRegion(cache, ds.getName(), rt, name);
        
        rss = region.getSnapshotService();
        SnapshotOptions<Integer, MyObject> options = rss.createOptions().setFilter(oops);
        
        boolean caughtException = false;
        try {
          rss.load(f, SnapshotFormat.GEMFIRE, options);
        } catch (RuntimeException e) {
          caughtException = true;
        }

        assertTrue(caughtException);
        assertEquals("Comparison failure for " + rt.name() + "/" + st.name(), 0, region.size());
      }
    }
  }

  @Test
  public void testInvalidate() throws Exception {
    Region<Integer, MyObject> region = rgen.createRegion(cache, ds.getName(), RegionType.REPLICATE, "test");
    MyObject obj = rgen.createData(SerializationType.SERIALIZABLE, 1, "invalidated value");
    
    region.put(1, obj);
    region.invalidate(1);
    
    region.getSnapshotService().save(f, SnapshotFormat.GEMFIRE);
    region.getSnapshotService().load(f, SnapshotFormat.GEMFIRE);
    
    assertTrue(region.containsKey(1));
    assertFalse(region.containsValueForKey(1));
    assertNull(region.get(1));
  }
  
  @Test
  public void testDSID() throws Exception {
    cache.close();

    CacheFactory cf = new CacheFactory().set("mcast-port", "0")
        .set("log-level", "error")
        .setPdxSerializer(new MyPdxSerializer())
        .set("distributed-system-id", "1");
    cache = cf.create();

    RegionType rt = RegionType.REPLICATE;
    SerializationType st = SerializationType.PDX_SERIALIZER;

    String name = "test-" + rt.name() + "-" + st.name() + "-dsid";
    Region<Integer, MyObject> region = rgen.createRegion(cache, ds.getName(),
        rt, name);
    final Map<Integer, MyObject> expected = createExpected(st);

    region.putAll(expected);
    region.getSnapshotService().save(f, SnapshotFormat.GEMFIRE);

    cache.close();

    // change the DSID from 1 -> 100
    CacheFactory cf2 = new CacheFactory().set("mcast-port", "0")
        .set("log-level", "error")
        .setPdxSerializer(new MyPdxSerializer())
        .set("distributed-system-id", "100");
    cache = cf2.create();

    final Map<Integer, Object> read = new HashMap<Integer, Object>();
    SnapshotIterator<Integer, Object> iter = SnapshotReader.read(f);
    try {
      while (iter.hasNext()) {
        Entry<Integer, Object> entry = iter.next();
        read.put(entry.getKey(), entry.getValue());
      }
      assertEquals("Comparison failure for " + rt.name() + "/" + st.name(),
          expected, read);
    } finally {
      iter.close();
    }
  }      

  @Before
  public void setUp() throws Exception {
    super.setUp();
    f = new File(snaps, "test.snapshot");
  }
}
