/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geode.cache.client.function;

import org.apache.geode.cache.RegionService;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.common.function.FunctionServiceBase;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.FunctionException;

import java.util.Properties;

public interface FunctionService extends FunctionServiceBase {

  /**
   * Returns an {@link Execution} object that can be used to execute a data independent function on
   * a server in the provided {@link Pool}.
   * <p>
   * If the server goes down while dispatching or executing the function, an Exception will be
   * thrown.
   * @param pool from which to chose a server for execution
   * @return Execution
   * @throws FunctionException if Pool instance passed in is null
   * @since GemFire 6.0
   */
  Execution onServer(Pool pool);

  /**
   * Returns an {@link Execution} object that can be used to execute a data independent function on
   * all the servers in the provided {@link Pool}. If one of the servers goes down while dispatching
   * or executing the function on the server, an Exception will be thrown.
   * @param pool the set of servers to execute the function
   * @return Execution
   * @throws FunctionException if Pool instance passed in is null
   * @since GemFire 6.0
   */
  Execution onServers(Pool pool);

  /**
   * Returns an {@link Execution} object that can be used to execute a data independent function on
   * a server that the given cache is connected to.
   * <p>
   * If the server goes down while dispatching or executing the function, an Exception will be
   * thrown.
   * @param regionService obtained from {@link ClientCacheFactory#create} or
   * {@link ClientCache#createAuthenticatedView(Properties)}.
   * @return Execution
   * @throws FunctionException if cache is null, is not on a client, or it does not have a default
   * pool
   * @since GemFire 6.5
   */
  Execution onServer(RegionService regionService);

  /**
   * Returns an {@link Execution} object that can be used to execute a data independent function on
   * all the servers that the given cache is connected to. If one of the servers goes down while
   * dispatching or executing the function on the server, an Exception will be thrown.
   * @param regionService obtained from {@link ClientCacheFactory#create} or
   * {@link ClientCache#createAuthenticatedView(Properties)}.
   * @return Execution
   * @throws FunctionException if cache is null, is not on a client, or it does not have a default
   * pool
   * @since GemFire 6.5
   */
  Execution onServers(RegionService regionService);
}