/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */

package com.gemstone.gemfire.internal.cache.extension.mock;

import java.util.List;
import java.util.Set;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.ResultCollector;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.management.cli.CliMetaData;
import com.gemstone.gemfire.management.cli.Result;
import com.gemstone.gemfire.management.cli.Result.Status;
import com.gemstone.gemfire.management.internal.cli.CliUtil;
import com.gemstone.gemfire.management.internal.cli.functions.CliFunctionResult;
import com.gemstone.gemfire.management.internal.cli.result.ResultBuilder;
import com.gemstone.gemfire.management.internal.cli.result.TabularResultData;
import com.gemstone.gemfire.management.internal.configuration.SharedConfigurationWriter;
import com.gemstone.gemfire.management.internal.configuration.domain.XmlEntity;

/**
 * Mock Extension gfsh commands.
 * 
 * @author jbarrett@pivotal.io
 *
 * @since 8.1
 */
public class MockExtensionCommands implements CommandMarker {

  public static final String OPTION_VALUE = "value";

  public static final String OPTION_REGION_NAME = "region-name";

  public static final String CREATE_MOCK_REGION_EXTENSION = "create mock region extension";

  public static final String ALTER_MOCK_REGION_EXTENSION = "alter mock region extension";

  public static final String DESTROY_MOCK_REGION_EXTENSION = "destroy mock region extension";

  public static final String CREATE_MOCK_CACHE_EXTENSION = "create mock cache extension";

  public static final String ALTER_MOCK_CACHE_EXTENSION = "alter mock cache extension";

  public static final String DESTROY_MOCK_CACHE_EXTENSION = "destroy mock cache extension";

  /**
   * Creates a {@link MockRegionExtension} on the given <code>regionName</code>.
   * 
   * @param regionName
   *          {@link Region} name on which to create {@link MockRegionExtension}
   *          .
   * @param value
   *          {@link String} value to set on
   *          {@link MockRegionExtension#setValue(String)}.
   * @return {@link Result}
   * @since 8.1
   */
  @CliCommand(value = CREATE_MOCK_REGION_EXTENSION)
  @CliMetaData(writesToSharedConfiguration = true)
  public Result createMockRegionExtension(@CliOption(key = OPTION_REGION_NAME, mandatory = true) final String regionName,
      @CliOption(key = OPTION_VALUE, mandatory = true) final String value) {
    return executeFunctionOnAllMembersTabulateResultPersist(CreateMockRegionExtensionFunction.INSTANCE, true,
        CreateMockRegionExtensionFunction.toArgs(regionName, value));
  }

  /**
   * Alters a {@link MockRegionExtension} on the given <code>regionName</code>.
   * 
   * @param regionName
   *          {@link Region} name on which to create {@link MockRegionExtension}
   *          .
   * @param value
   *          {@link String} value to set on
   *          {@link MockRegionExtension#setValue(String)}.
   * @return {@link Result}
   * @since 8.1
   */
  @CliCommand(value = ALTER_MOCK_REGION_EXTENSION)
  @CliMetaData(writesToSharedConfiguration = true)
  public Result alterMockRegionExtension(@CliOption(key = OPTION_REGION_NAME, mandatory = true) final String regionName,
      @CliOption(key = OPTION_VALUE, mandatory = true) final String value) {
    return executeFunctionOnAllMembersTabulateResultPersist(AlterMockRegionExtensionFunction.INSTANCE, true,
        AlterMockRegionExtensionFunction.toArgs(regionName, value));
  }

  /**
   * Destroy the {@link MockRegionExtension} on the given
   * <code>regionName</code>.
   * 
   * @param regionName
   *          {@link Region} name on which to create {@link MockRegionExtension}
   *          .
   * @return {@link Result}
   * @since 8.1
   */
  @CliCommand(value = DESTROY_MOCK_REGION_EXTENSION)
  @CliMetaData(writesToSharedConfiguration = true)
  public Result destroyMockRegionExtension(@CliOption(key = OPTION_REGION_NAME, mandatory = true) final String regionName) {
    return executeFunctionOnAllMembersTabulateResultPersist(DestroyMockRegionExtensionFunction.INSTANCE, true,
        DestroyMockRegionExtensionFunction.toArgs(regionName));
  }

  /**
   * Creates a {@link MockCacheExtension}.
   * 
   * @param value
   *          {@link String} value to set on
   *          {@link MockCacheExtension#setValue(String)}.
   * @return {@link Result}
   * @since 8.1
   */
  @CliCommand(value = CREATE_MOCK_CACHE_EXTENSION)
  @CliMetaData(writesToSharedConfiguration = true)
  public Result createMockCacheExtension(@CliOption(key = OPTION_VALUE, mandatory = true) final String value) {
    return executeFunctionOnAllMembersTabulateResultPersist(CreateMockCacheExtensionFunction.INSTANCE, true, CreateMockCacheExtensionFunction.toArgs(value));
  }

  /**
   * Alter a {@link MockCacheExtension}.
   * 
   * @param value
   *          {@link String} value to set on
   *          {@link MockCacheExtension#setValue(String)}.
   * @return {@link Result}
   * @since 8.1
   */
  @CliCommand(value = ALTER_MOCK_CACHE_EXTENSION)
  @CliMetaData(writesToSharedConfiguration = true)
  public Result alterMockCacheExtension(@CliOption(key = OPTION_VALUE, mandatory = true) final String value) {
    return executeFunctionOnAllMembersTabulateResultPersist(AlterMockCacheExtensionFunction.INSTANCE, true, AlterMockCacheExtensionFunction.toArgs(value));
  }

  /**
   * Destroy a {@link MockCacheExtension}.
   * 
   * @return {@link Result}
   * @since 8.1
   */
  @CliCommand(value = DESTROY_MOCK_CACHE_EXTENSION)
  @CliMetaData(writesToSharedConfiguration = true)
  public Result destroyMockCacheExtension() {
    return executeFunctionOnAllMembersTabulateResultPersist(DestroyMockCacheExtensionFunction.INSTANCE, false);
  }

  /**
   * Call <code>function</code> with <code>args</code> on all members, tabulate
   * results and persist shared config if changed.
   * 
   * @param function
   *          {@link Function} to execute.
   * @param addXmlElement
   *          If <code>true</code> then add result {@link XmlEntity} to the
   *          config, otherwise delete it.
   * @param args
   *          Arguments to pass to function.
   * @return {@link TabularResultData}
   * @since 8.1
   */
  protected Result executeFunctionOnAllMembersTabulateResultPersist(final Function function, final boolean addXmlElement, final Object... args) {
    final Cache cache = CacheFactory.getAnyInstance();
    final Set<DistributedMember> members = CliUtil.getAllNormalMembers(cache);

    @SuppressWarnings("unchecked")
    final ResultCollector<CliFunctionResult, List<CliFunctionResult>> resultCollector = (ResultCollector<CliFunctionResult, List<CliFunctionResult>>) CliUtil
        .executeFunction(function, args, members);
    final List<CliFunctionResult> functionResults = (List<CliFunctionResult>) resultCollector.getResult();

    XmlEntity xmlEntity = null;
    final TabularResultData tabularResultData = ResultBuilder.createTabularResultData();
    final String errorPrefix = "ERROR: ";
    for (CliFunctionResult functionResult : functionResults) {
      boolean success = functionResult.isSuccessful();
      tabularResultData.accumulate("Member", functionResult.getMemberIdOrName());
      if (success) {
        tabularResultData.accumulate("Status", functionResult.getMessage());
        xmlEntity = functionResult.getXmlEntity();
      } else {
        tabularResultData.accumulate("Status", errorPrefix + functionResult.getMessage());
        tabularResultData.setStatus(Status.ERROR);
      }
    }

    final Result result = ResultBuilder.buildResult(tabularResultData);

    System.out.println("MockExtensionCommands: persisting xmlEntity=" + xmlEntity);
    if (null != xmlEntity) {
      if (addXmlElement) {
        result.setCommandPersisted((new SharedConfigurationWriter()).addXmlEntity(xmlEntity, null));
      } else {
        result.setCommandPersisted((new SharedConfigurationWriter()).deleteXmlEntity(xmlEntity, null));
      }
    }

    return result;
  }

}