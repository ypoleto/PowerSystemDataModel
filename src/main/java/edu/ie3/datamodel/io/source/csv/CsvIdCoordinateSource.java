/*
 * © 2021. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
*/
package edu.ie3.datamodel.io.source.csv;

import edu.ie3.datamodel.exceptions.SourceException;
import edu.ie3.datamodel.io.factory.SimpleFactoryData;
import edu.ie3.datamodel.io.factory.timeseries.IdCoordinateFactory;
import edu.ie3.datamodel.io.source.IdCoordinateSource;
import edu.ie3.datamodel.utils.Try;
import edu.ie3.util.geo.CoordinateDistance;
import edu.ie3.util.geo.GeoUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.measure.quantity.Length;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

/**
 * Implementation of {@link IdCoordinateSource} to read the mapping between coordinate id and actual
 * coordinate from csv file and build a mapping from it.
 */
public class CsvIdCoordinateSource implements IdCoordinateSource {

  protected static final Logger log = LoggerFactory.getLogger(CsvIdCoordinateSource.class);

  private static final String COORDINATE_ID_MAPPING = "coordinate id mapping";

  /** Mapping in both ways (id -> coordinate) and (coordinate -> id) have to be unique */
  private final Map<Integer, Point> idToCoordinate;

  private final Map<Point, Integer> coordinateToId;

  private final CsvDataSource dataSource;
  private final IdCoordinateFactory factory;

  public CsvIdCoordinateSource(IdCoordinateFactory factory, CsvDataSource dataSource)
      throws SourceException {
    this.factory = factory;
    this.dataSource = dataSource;

    /* set up the coordinate id to lat/long mapping */
    idToCoordinate = setupIdToCoordinateMap();
    coordinateToId = invert(idToCoordinate);
  }

  /**
   * Read in and process the mapping
   *
   * @return Mapping from coordinate id to coordinate
   */
  private Map<Integer, Point> setupIdToCoordinateMap() throws SourceException {
    return Try.scanStream(
            buildStreamWithFieldsToAttributesMap()
                .map(fieldToValues -> new SimpleFactoryData(fieldToValues, Pair.class))
                .map(factory::get),
            "Pair<Integer, Point>")
        .transform(
            s -> s.collect(Collectors.toMap(Pair::getKey, Pair::getValue)), SourceException::new)
        .getOrThrow();
  }

  /**
   * Inverts the mapping, so that former values map to keys
   *
   * @param map Mapping in the "right" direction
   * @param <V> Type of values
   * @param <K> Type of keys
   * @return Mapping in the "left" direction (Bad joke, I know...)
   */
  private <V, K> Map<V, K> invert(Map<K, V> map) {
    Map<V, K> inv = new HashMap<>();
    for (Map.Entry<K, V> entry : map.entrySet()) inv.put(entry.getValue(), entry.getKey());
    return inv;
  }

  @Override
  public Optional<Point> getCoordinate(int id) {
    return Optional.ofNullable(idToCoordinate.get(id));
  }

  @Override
  public Collection<Point> getCoordinates(int... ids) {
    return Arrays.stream(ids)
        .mapToObj(this::getCoordinate)
        .flatMap(Optional::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Optional<Integer> getId(Point coordinate) {
    return Optional.ofNullable(coordinateToId.get(coordinate));
  }

  @Override
  public Collection<Point> getAllCoordinates() {
    return coordinateToId.keySet();
  }

  @Override
  public List<CoordinateDistance> getNearestCoordinates(Point coordinate, int n) {
    Set<Point> points = coordinateToId.keySet();

    if (idToCoordinate.size() > n) {
      ArrayList<Point> foundPoints = new ArrayList<>();
      ComparableQuantity<Length> distance = Quantities.getQuantity(10000, Units.METRE);

      // extends the search radius until n points are found
      while (foundPoints.size() < n) {
        foundPoints.clear();
        distance = distance.multiply(2);

        Envelope envelope = GeoUtils.calculateBoundingBox(coordinate, distance);
        points.stream()
            .filter(point -> envelope.contains(point.getCoordinate()))
            .forEach(foundPoints::add);
      }

      // replaces all point with smaller size of found points
      points.clear();
      points.addAll(foundPoints);
    }

    return calculateCoordinateDistances(coordinate, n, points);
  }

  @Override
  public List<CoordinateDistance> getClosestCoordinates(
      Point coordinate, int n, ComparableQuantity<Length> distance) {
    Set<Point> points = coordinateToId.keySet();

    Envelope envelope = GeoUtils.calculateBoundingBox(coordinate, distance);
    Set<Point> reducedPoints =
        points.stream()
            .filter(point -> envelope.contains(point.getCoordinate()))
            .collect(Collectors.toSet());
    return calculateCoordinateDistances(coordinate, n, reducedPoints);
  }

  public int getCoordinateCount() {
    return idToCoordinate.keySet().size();
  }

  /**
   * Build a stream with mappings from field identifiers to attributes
   *
   * @return Stream with mappings from field identifiers to attributes
   */
  protected Stream<Map<String, String>> buildStreamWithFieldsToAttributesMap() {
    try (BufferedReader reader = dataSource.connector.initIdCoordinateReader()) {
      final String[] headline = dataSource.parseCsvRow(reader.readLine(), dataSource.csvSep);

      // by default try-with-resources closes the reader directly when we leave this method (which
      // is wanted to avoid a lock on the file), but this causes a closing of the stream as well.
      // As we still want to consume the data at other places, we start a new stream instead of
      // returning the original one
      Collection<Map<String, String>> allRows =
          dataSource.csvRowFieldValueMapping(reader, headline);

      Function<Map<String, String>, String> idExtractor =
          fieldToValues -> fieldToValues.get(factory.getIdField());
      Set<Map<String, String>> withDistinctCoordinateId =
          dataSource.distinctRowsWithLog(
              allRows, idExtractor, COORDINATE_ID_MAPPING, "coordinate id");
      Function<Map<String, String>, String> coordinateExtractor =
          fieldToValues ->
              fieldToValues
                  .get(factory.getLatField())
                  .concat(fieldToValues.get(factory.getLonField()));
      return dataSource
          .distinctRowsWithLog(
              withDistinctCoordinateId, coordinateExtractor, COORDINATE_ID_MAPPING, "coordinate")
          .parallelStream();
    } catch (IOException e) {
      log.error("Cannot read the file for coordinate id to coordinate mapping.", e);
    }
    return Stream.empty();
  }
}
