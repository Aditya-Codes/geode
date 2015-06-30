package com.gemstone.gemfire.internal.redis.executor.string;

import java.util.List;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.internal.redis.ByteArrayWrapper;
import com.gemstone.gemfire.internal.redis.Command;
import com.gemstone.gemfire.internal.redis.Coder;
import com.gemstone.gemfire.internal.redis.ExecutionHandlerContext;
import com.gemstone.gemfire.internal.redis.RedisConstants.ArityDef;

public class SetExecutor extends StringExecutor {

  private final String SUCCESS = "OK";

  private final int VALUE_INDEX = 2;

  @Override
  public void executeCommand(Command command, ExecutionHandlerContext context) {
    List<byte[]> commandElems = command.getProcessedCommand();

    Region<ByteArrayWrapper, ByteArrayWrapper> r = context.getRegionCache().getStringsRegion();

    if (commandElems.size() < 3) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ArityDef.SET));
      return;
    }

    ByteArrayWrapper key = command.getKey();
    checkDataType(key, context);
    byte[] value = commandElems.get(VALUE_INDEX);
    ByteArrayWrapper valueWrapper = new ByteArrayWrapper(value);

    boolean NX = false; // Set only if not exists
    boolean XX = false; // Set only if exists
    long expiration = 0L;

    if (commandElems.size() >= 6) {
      String elem4;
      String elem5;
      String elem6;

      elem4 = Coder.bytesToString(commandElems.get(3));
      elem5 = Coder.bytesToString(commandElems.get(4));
      elem6 = Coder.bytesToString(commandElems.get(5));

      if (elem4.equalsIgnoreCase("XX") || elem6.equalsIgnoreCase("XX"))
        XX = true;
      else if (elem4.equalsIgnoreCase("NX") || elem6.equalsIgnoreCase("NX"))
        NX = true;

      if (elem4.equalsIgnoreCase("PX"))
        expiration = getExpirationMillis(elem4, elem5);
      else if (elem5.equalsIgnoreCase("PX"))
        expiration = getExpirationMillis(elem5, elem6);
      else if (elem4.equalsIgnoreCase("EX"))
        expiration = getExpirationMillis(elem4, elem5);
      else if (elem5.equalsIgnoreCase("EX"))
        expiration = getExpirationMillis(elem5, elem6);

    } else if (commandElems.size() >= 5) {
      String elem4;
      String expiry;

      elem4 = Coder.bytesToString(commandElems.get(3));
      expiry = Coder.bytesToString(commandElems.get(4));

      expiration = getExpirationMillis(elem4, expiry);
    } else if (commandElems.size() >= 4) {
      byte[] elem4 = commandElems.get(3);
      if (elem4.length == 2 && Character.toUpperCase(elem4[1]) == 'X') {
        if (Character.toUpperCase(elem4[0]) == 'N')
          NX = true;
        else if (Character.toUpperCase(elem4[0]) == 'X')
          XX = true;
      }
    }

    boolean keyWasSet = false;

    if (NX)
      keyWasSet = setNX(r, command, key, valueWrapper, context);
    else if (XX)
      keyWasSet = setXX(r, command, key, valueWrapper, context);
    else {
      checkAndSetDataType(key, context);
      r.put(key, valueWrapper);
      command.setResponse(Coder.getSimpleStringResponse(context.getByteBufAllocator(), SUCCESS));
      keyWasSet = true;
    }

    if (keyWasSet && expiration > 0L) {
      context.getRegionCache().setExpiration(key, expiration);
    }

  }

  private boolean setNX(Region<ByteArrayWrapper, ByteArrayWrapper> r, Command command, ByteArrayWrapper key, ByteArrayWrapper valueWrapper, ExecutionHandlerContext context) {
    checkAndSetDataType(key, context);
    Object oldValue = r.putIfAbsent(key, valueWrapper);
    if (oldValue != null) {
      command.setResponse(Coder.getNilResponse(context.getByteBufAllocator()));
      return false;
    } else {
      command.setResponse(Coder.getSimpleStringResponse(context.getByteBufAllocator(), SUCCESS));
      return true;
    }
  }

  private boolean setXX(Region<ByteArrayWrapper, ByteArrayWrapper> r, Command command, ByteArrayWrapper key, ByteArrayWrapper valueWrapper, ExecutionHandlerContext context) {
    if(r.containsKey(key)) {
      checkAndSetDataType(key, context);
      r.put(key, valueWrapper);
      command.setResponse(Coder.getSimpleStringResponse(context.getByteBufAllocator(), SUCCESS));
      return true;
    } else {
      command.setResponse(Coder.getNilResponse(context.getByteBufAllocator()));
      return false;
    }
  }

  private long getExpirationMillis(String expx, String expirationString) {
    long expiration = 0L;
    try {
      expiration = Long.parseLong(expirationString);
    } catch (NumberFormatException e) {
      return 0L;
    }

    if (expx.equalsIgnoreCase("EX"))
      return expiration * millisInSecond;
    else if (expx.equalsIgnoreCase("PX"))
      return expiration;
    else
      return 0L;
  }

}
