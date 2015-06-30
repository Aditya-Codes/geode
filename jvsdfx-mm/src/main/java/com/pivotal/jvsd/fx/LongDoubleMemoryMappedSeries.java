package com.pivotal.jvsd.fx;

import java.nio.ByteBuffer;

import com.pivotal.javafx.scene.chart.ByteBufferNumberSeries;
import com.pivotal.javafx.scene.chart.Data;

import javafx.collections.ObservableList;

/**
 * A named series of data items
 */
public class LongDoubleMemoryMappedSeries extends ByteBufferNumberSeries<Number, Number> {

  // -------------- CONSTRUCTORS ----------------------------------------------

  /**
   * Construct a empty series
   */
  public LongDoubleMemoryMappedSeries() {
    super();
  }

  /**
   * Constructs a Series and populates it with the given {@link ObservableList}
   * data.
   *
   * @param data
   *          ObservableList of MultiAxisChart.Data
   */
  public LongDoubleMemoryMappedSeries(ByteBuffer buffer) {
    super(buffer);
  }

  /**
   * Constructs a named Series and populates it with the given
   * {@link ObservableList} data.
   *
   * @param name
   *          a name for the series
   * @param data
   *          ObservableList of MultiAxisChart.Data
   */
  public LongDoubleMemoryMappedSeries(String name, ByteBuffer buffer) {
    super(name, buffer);
  }

  // BEGIN FORMAT SPECIFIC
  private static final int DATA_X_OFFSET = 0;
  private static final int DATA_X_LENGTH = 8;
  private static final int DATA_Y_OFFSET = DATA_X_OFFSET + DATA_X_LENGTH;
  private static final int DATA_Y_LENGTH = 8;
  private static final int DATA_WIDTH = DATA_Y_OFFSET + DATA_Y_LENGTH;
  
  // TODO Make these Abstract
  @Override
  public int getDataSize() {
    return getBuffer().limit() / DATA_WIDTH;
  }
  
  // TODO Make these Abstract
  private long getXRaw(final int index) {
    return buffer.getLong(index * DATA_WIDTH + DATA_X_OFFSET);
  }

  // TODO Make these Abstract
  private double getYRaw(final int index) {
    return buffer.getDouble(index * DATA_WIDTH + DATA_Y_OFFSET);
  }

  // END FORMAT SPECIFIC
 
  @Override
  final protected Data<Number,Number> createData(int index) {
    return new Data<>(getXRaw(index), getYRaw(index));
  };
    
  @Override
  final protected Long convertX(final double x) {
    return (long) x;
  }
  
  @Override
  final protected Double convertY(final double y) {
    return y;
  }

  @Override
  final protected double getX(final int index) {
    return (double) getXRaw(index);
  };

  @Override
  final protected double getY(final int index) {
    return (double) getYRaw(index);
  }

}