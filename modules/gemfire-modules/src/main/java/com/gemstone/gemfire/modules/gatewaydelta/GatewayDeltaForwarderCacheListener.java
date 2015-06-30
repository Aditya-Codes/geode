/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.modules.gatewaydelta;

import java.util.Properties;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.InterestPolicy;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionFactory;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache.SerializedCacheValue;
import com.gemstone.gemfire.cache.SubscriptionAttributes;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.internal.cache.EntryEventImpl;
import com.gemstone.gemfire.internal.cache.LocalRegion;

public class GatewayDeltaForwarderCacheListener extends CacheListenerAdapter<String,GatewayDelta> implements Declarable {

  private final Cache cache;
   
  private LocalRegion gatewayDeltaRegion;
  
  public GatewayDeltaForwarderCacheListener() {
    this(CacheFactory.getAnyInstance());
  }
  
  public GatewayDeltaForwarderCacheListener(Cache cache) {
    this.cache = cache;
  }
  
  @SuppressWarnings("unchecked")
  public void afterCreate(EntryEvent<String,GatewayDelta> event) {
    // If the event is from the local site, create a 'create' event and send it to the
    // gateway delta region
    if (event.getCallbackArgument() == null) {
      if (this.cache.getLogger().fineEnabled()) {
        StringBuilder builder = new StringBuilder();
        builder
          .append("GatewayDeltaForwarderCacheListener: Received create event for ")
          .append(event.getKey())
          .append("->")
          .append(event.getNewValue())
          .append(" that originated in the local site. Sending it to the remote site.");
        this.cache.getLogger().fine(builder.toString());
      }
      
      // Distribute the create event to the gateway hub(s)
      String regionName = event.getRegion().getFullPath();
      String sessionId = event.getKey();
      SerializedCacheValue scv = event.getSerializedNewValue();
      if (scv == null) {
        getGatewayDeltaRegion().put(sessionId, new GatewayDeltaCreateEvent(regionName, sessionId, EntryEventImpl.serialize(event.getNewValue())));
      } else {
        System.out.println("GatewayDeltaForwarderCacheListener event.getSerializedNewValue().getSerializedValue(): " + event.getSerializedNewValue().getSerializedValue());
        getGatewayDeltaRegion().put(sessionId, new GatewayDeltaCreateEvent(regionName, sessionId, scv.getSerializedValue()));
      }
    } else {
      if (this.cache.getLogger().fineEnabled()) {
        StringBuilder builder = new StringBuilder();
        builder
          .append("GatewayDeltaForwarderCacheListener: Received create event for ")
          .append(event.getKey())
          .append("->")
          .append(event.getNewValue())
          .append(" that originated in the remote site.");
        this.cache.getLogger().fine(builder.toString());
      }
    }
  }
  
  public void afterUpdate(EntryEvent<String,GatewayDelta> event) {
    //System.out.println("GatewayDeltaForwarderCacheListener.afterUpdate: " + event);
    // If the event is from the local site, create an 'update' event and send it to the
    // gateway delta region
    if (event.getCallbackArgument() == null) {
      if (this.cache.getLogger().fineEnabled()) {
        StringBuilder builder = new StringBuilder();
        builder
          .append("GatewayDeltaForwarderCacheListener: Received update event for ")
          .append(event.getKey())
          .append("->")
          .append(event.getNewValue())
          .append(" that originated in the local site. Sending it to the remote site.");
        this.cache.getLogger().fine(builder.toString());
      }
      
      // Distribute the update event to the gateway hub(s)
      GatewayDelta session = event.getNewValue();
      getGatewayDeltaRegion().put(event.getKey(), session.getCurrentGatewayDeltaEvent());
      
      // Reset the current delta
      session.setCurrentGatewayDeltaEvent(null);
    } else {
      if (this.cache.getLogger().fineEnabled()) {
        StringBuilder builder = new StringBuilder();
        builder
          .append("GatewayDeltaForwarderCacheListener: Received update event for ")
          .append(event.getKey())
          .append("->")
          .append(event.getNewValue())
          .append(" that originated in the remote site.");
        this.cache.getLogger().fine(builder.toString());
      }
    }
  }
  
  public void afterDestroy(EntryEvent<String,GatewayDelta> event) {
    // If the event is from the local site, create a 'destroy' event and send it to the
    // gateway delta region
    if (event.getCallbackArgument() != null) {
      if (this.cache.getLogger().fineEnabled()) {
        StringBuilder builder = new StringBuilder();
        builder
          .append("GatewayDeltaForwarderCacheListener: Received destroy event for ")
          .append(event.getKey())
          .append("->")
          .append(event.getNewValue())
          .append(" that originated in the local site. Sending it to the remote site.");
        this.cache.getLogger().fine(builder.toString());
      }
      
      // Distribute the destroy event to the gateway hub(s)
      String sessionId = event.getKey();
      getGatewayDeltaRegion().put(sessionId, new GatewayDeltaDestroyEvent(event.getRegion().getFullPath(), sessionId));
    } else {
      if (this.cache.getLogger().fineEnabled()) {
        StringBuilder builder = new StringBuilder();
        builder
          .append("GatewayDeltaForwarderCacheListener: Received destroy event for session ")
          .append(event.getKey())
          .append(" that either expired or originated in the remote site.");
        this.cache.getLogger().fine(builder.toString());
      }
    }
  }

  public void init(Properties p) {
  }
  
  private LocalRegion getGatewayDeltaRegion() {
    if (this.gatewayDeltaRegion == null) {
      this.gatewayDeltaRegion = createOrRetrieveGatewayDeltaRegion();
    }
    return this.gatewayDeltaRegion;
  }
  
  @SuppressWarnings("unchecked")
  private LocalRegion createOrRetrieveGatewayDeltaRegion() {
    Region region = this.cache.getRegion(GatewayDelta.GATEWAY_DELTA_REGION_NAME);
    if (region == null) {
      region = new RegionFactory()
        .setScope(Scope.LOCAL)
        .setDataPolicy(DataPolicy.EMPTY)
        .setSubscriptionAttributes(new SubscriptionAttributes(InterestPolicy.ALL))
        .setEnableGateway(true)
        .addCacheListener(new GatewayDeltaEventApplicationCacheListener())
        .create(GatewayDelta.GATEWAY_DELTA_REGION_NAME);
    }
    if (this.cache.getLogger().fineEnabled()) {
      StringBuilder builder = new StringBuilder();
      builder
        .append("GatewayDeltaForwarderCacheListener: Created gateway delta region: ")
        .append(region);
      this.cache.getLogger().fine(builder.toString());
    }
    return (LocalRegion) region;
  }
  
  public boolean equals(Object obj) {
    // This method is only implemented so that RegionCreator.validateRegion works properly.
    // The CacheListener comparison fails because two of these instances are not equal.
    if (this == obj) {
      return true;
    }

    if (obj == null || !(obj instanceof GatewayDeltaForwarderCacheListener)) {
      return false;
    }
    
    return true;
  }
}