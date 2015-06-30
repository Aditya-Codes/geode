/*
 * =========================================================================
 *  Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 *  This product is protected by U.S. and international copyright
 *  and intellectual property laws. Pivotal products are covered by
 *  more patents listed at http://www.pivotal.io/patents.
 * ========================================================================
 */

package com.gemstone.gemfire.management.internal.cli.commands;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.execute.Execution;
import com.gemstone.gemfire.cache.execute.FunctionInvocationTargetException;
import com.gemstone.gemfire.cache.execute.ResultCollector;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.internal.cache.execute.AbstractExecution;
import com.gemstone.gemfire.internal.util.CollectionUtils;
import com.gemstone.gemfire.management.internal.cli.domain.IndexDetails;
import com.gemstone.gemfire.management.internal.cli.functions.ListIndexFunction;
import com.gemstone.gemfire.test.junit.categories.UnitTest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * The IndexCommandsJUnitTest class is a test suite of test cases testing the contract and functionality of the
 * IndexCommands class.
 * </p>
 * @author John Blum
 * @see com.gemstone.gemfire.management.internal.cli.commands.IndexCommands
 * @see com.gemstone.gemfire.management.internal.cli.domain.IndexDetails
 * @see com.gemstone.gemfire.management.internal.cli.functions.ListIndexFunction
 * @see org.jmock.Expectations
 * @see org.jmock.Mockery
 * @see org.jmock.lib.legacy.ClassImposteriser
 * @see org.junit.Assert
 * @see org.junit.Test
 * @since 7.0
 */
@Category(UnitTest.class)
public class IndexCommandsJUnitTest {

  private Mockery mockContext;

  @Before
  public void setup() {
    mockContext = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
  }

  @After
  public void tearDown() {
    mockContext.assertIsSatisfied();
    mockContext = null;
  }

  protected IndexCommands createIndexCommands(final Cache cache, final Execution functionExecutor) {
    return new TestIndexCommands(cache, functionExecutor);
  }

  protected IndexDetails createIndexDetails(final String memberId, final String regionPath, final String indexName) {
    return new IndexDetails(memberId, regionPath, indexName);
  }

  @Test
  public void testGetIndexListing() {
    final Cache mockCache = mockContext.mock(Cache.class, "Cache");

    final AbstractExecution mockFunctionExecutor = mockContext.mock(AbstractExecution.class, "Function Executor");

    final ResultCollector mockResultCollector = mockContext.mock(ResultCollector.class, "ResultCollector");

    final IndexDetails indexDetails1 = createIndexDetails("memberOne", "/Employees", "empIdIdx");
    final IndexDetails indexDetails2 = createIndexDetails("memberOne", "/Employees", "empLastNameIdx");
    final IndexDetails indexDetails3 = createIndexDetails("memberTwo", "/Employees", "empDobIdx");

    final List<IndexDetails> expectedIndexDetails = Arrays.asList(indexDetails1, indexDetails2, indexDetails3);

    final List<Set<IndexDetails>> results = new ArrayList<Set<IndexDetails>>(2);

    results.add(CollectionUtils.asSet(indexDetails2, indexDetails1));
    results.add(CollectionUtils.asSet(indexDetails3));

    mockContext.checking(new Expectations() {{
      oneOf(mockFunctionExecutor).setIgnoreDepartedMembers(with(equal(true)));
      oneOf(mockFunctionExecutor).execute(with(aNonNull(ListIndexFunction.class)));
      will(returnValue(mockResultCollector));
      oneOf(mockResultCollector).getResult();
      will(returnValue(results));
    }});

    final IndexCommands commands = createIndexCommands(mockCache, mockFunctionExecutor);

    final List<IndexDetails> actualIndexDetails = commands.getIndexListing();

    assertNotNull(actualIndexDetails);
    assertEquals(expectedIndexDetails, actualIndexDetails);
  }

  @Test(expected = RuntimeException.class)
  public void testGetIndexListingThrowsRuntimeException() {
    final Cache mockCache = mockContext.mock(Cache.class, "Cache");

    final Execution mockFunctionExecutor = mockContext.mock(Execution.class, "Function Executor");

    mockContext.checking(new Expectations() {{
      oneOf(mockFunctionExecutor).execute(with(aNonNull(ListIndexFunction.class)));
      will(throwException(new RuntimeException("expected")));
    }});

    final IndexCommands commands = createIndexCommands(mockCache, mockFunctionExecutor);

    try {
      commands.getIndexListing();
    }
    catch (RuntimeException expected) {
      assertEquals("expected", expected.getMessage());
      throw expected;
    }
  }

  @Test
  public void testGetIndexListingReturnsFunctionInvocationTargetExceptionInResults() {
    final Cache mockCache = mockContext.mock(Cache.class, "Cache");

    final AbstractExecution mockFunctionExecutor = mockContext.mock(AbstractExecution.class, "Function Executor");

    final ResultCollector mockResultCollector = mockContext.mock(ResultCollector.class, "ResultCollector");

    final IndexDetails indexDetails = createIndexDetails("memberOne", "/Employees", "empIdIdx");

    final List<IndexDetails> expectedIndexDetails = Arrays.asList(indexDetails);

    final List<Object> results = new ArrayList<Object>(2);

    results.add(CollectionUtils.asSet(indexDetails));
    results.add(new FunctionInvocationTargetException("expected"));

    mockContext.checking(new Expectations() {{
      oneOf(mockFunctionExecutor).setIgnoreDepartedMembers(with(equal(true)));
      oneOf(mockFunctionExecutor).execute(with(aNonNull(ListIndexFunction.class)));
      will(returnValue(mockResultCollector));
      oneOf(mockResultCollector).getResult();
      will(returnValue(results));
    }});

    final IndexCommands commands = createIndexCommands(mockCache, mockFunctionExecutor);

    final List<IndexDetails> actualIndexDetails = commands.getIndexListing();

    assertNotNull(actualIndexDetails);
    assertEquals(expectedIndexDetails, actualIndexDetails);
  }

  protected static class TestIndexCommands extends IndexCommands {

    private final Cache cache;
    private final Execution functionExecutor;

    protected TestIndexCommands(final Cache cache, final Execution functionExecutor) {
      assert cache != null : "The Cache cannot be null!";
      assert functionExecutor != null : "The function executor cannot be null!";
      this.cache = cache;
      this.functionExecutor = functionExecutor;
    }

    @Override
    protected Cache getCache() {
      return this.cache;
    }

    @Override
    protected Set<DistributedMember> getMembers(final Cache cache) {
      assertSame(getCache(), cache);
      return Collections.emptySet();
    }

    @Override
    protected Execution getMembersFunctionExecutor(final Set<DistributedMember> members) {
      Assert.assertNotNull(members);
      return functionExecutor;
    }
  }

}
