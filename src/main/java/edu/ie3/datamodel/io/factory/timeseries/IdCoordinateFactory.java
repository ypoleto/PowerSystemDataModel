/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
*/
package edu.ie3.datamodel.io.factory.timeseries;

import edu.ie3.datamodel.io.factory.Factory;
import edu.ie3.datamodel.io.factory.SimpleFactoryData;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Point;

/**
 * Abstract class definition for a factory, that is able to build single mapping entries from
 * coordinate identifier to actual coordinate
 */
public abstract class IdCoordinateFactory
    extends Factory<Pair, SimpleFactoryData, Pair<Integer, Point>> {
  public IdCoordinateFactory() {
    super(Pair.class);
  }
}
