/*
 * © 2021. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
*/
package edu.ie3.datamodel.utils.validation;

import edu.ie3.datamodel.exceptions.InvalidEntityException;
import edu.ie3.datamodel.exceptions.InvalidGridException;
import edu.ie3.datamodel.exceptions.ValidationException;
import edu.ie3.datamodel.models.input.NodeInput;
import edu.ie3.datamodel.models.input.connector.*;
import edu.ie3.datamodel.models.input.connector.type.LineTypeInput;
import edu.ie3.datamodel.models.input.connector.type.Transformer2WTypeInput;
import edu.ie3.datamodel.models.input.connector.type.Transformer3WTypeInput;
import edu.ie3.datamodel.models.input.container.SubGridContainer;
import edu.ie3.datamodel.utils.Try;
import edu.ie3.datamodel.utils.Try.*;
import edu.ie3.util.geo.GeoUtils;
import edu.ie3.util.quantities.QuantityUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.measure.Quantity;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

public class ConnectorValidationUtils extends ValidationUtils {

  // allowed deviation of coordinates in degree for line position check
  private static final double ALLOWED_COORDINATE_ERROR = 0.000001d;
  // allowed deviation of length in meters for line length
  private static final double ALLOWED_LENGTH_ERROR = 50d;
  // allowed deviation of voltage in kV for transformer checks
  private static final double ALLOWED_VOLTAGE_ERROR = 1d;

  /** Private Constructor as this class is not meant to be instantiated */
  private ConnectorValidationUtils() {
    throw new IllegalStateException("Don't try and instantiate a Utility class.");
  }

  /**
   * Validates a connector if: <br>
   * - it is not null <br>
   * A "distribution" method, that forwards the check request to specific implementations to fulfill
   * the checking task, based on the class of the given object.
   *
   * @param connector Connector to validate
   * @return a list of try objects either containing a {@link InvalidEntityException} or an empty
   *     Success
   */
  protected static List<Try<Void, InvalidEntityException>> check(ConnectorInput connector) {
    Try<Void, InvalidEntityException> isNull = checkNonNull(connector, "a connector");

    if (isNull.isFailure()) {
      return List.of(isNull);
    }

    List<Try<Void, InvalidEntityException>> exceptions = new ArrayList<>();
    exceptions.add(connectsDifferentNodes(connector));

    // Further checks for subclasses
    if (LineInput.class.isAssignableFrom(connector.getClass())) {
      exceptions.addAll(checkLine((LineInput) connector));
    } else if (Transformer2WInput.class.isAssignableFrom(connector.getClass())) {
      exceptions.addAll(checkTransformer2W((Transformer2WInput) connector));
    } else if (Transformer3WInput.class.isAssignableFrom(connector.getClass())) {
      exceptions.addAll(checkTransformer3W((Transformer3WInput) connector));
    } else if (SwitchInput.class.isAssignableFrom(connector.getClass())) {
      exceptions.add(checkSwitch((SwitchInput) connector));
    } else {
      exceptions.add(
          new Failure<>(
              new InvalidEntityException(
                  "Validation failed due to: ", buildNotImplementedException(connector))));
    }

    return exceptions;
  }

  /**
   * Validates a line if: <br>
   * - {@link ConnectorValidationUtils#checkLineType(LineTypeInput)} confirms valid type properties
   * <br>
   * - it does not connect the same node <br>
   * - it connects nodes in the same subnet <br>
   * - it connects nodes in the same voltage level <br>
   * - its line length has a positive value <br>
   * - its length equals the sum of calculated distances between points of LineString <br>
   * - its coordinates of start and end point equal coordinates of nodes
   *
   * @param line Line to validate
   * @return a list of try objects either containing an {@link InvalidEntityException} or an empty
   *     Success
   */
  private static List<Try<Void, InvalidEntityException>> checkLine(LineInput line) {
    List<Try<Void, InvalidEntityException>> exceptions =
        new ArrayList<>(checkLineType(line.getType()));

    exceptions.addAll(
        Try.ofVoid(
            InvalidEntityException.class,
            () -> connectsNodesInDifferentSubnets(line, false),
            () -> connectsNodesWithDifferentVoltageLevels(line, false),
            () -> detectZeroOrNegativeQuantities(new Quantity<?>[] {line.getLength()}, line)));

    /* these two won't throw exceptions and will only log */
    coordinatesOfLineEqualCoordinatesOfNodes(line);
    lineLengthMatchesDistancesBetweenPointsOfLineString(line);

    return exceptions;
  }

  /**
   * Validates a line type if: <br>
   * - it is not null <br>
   * - B is greater/equal to 0 (Phase-to-ground susceptance per length) <br>
   * - G is greater/equal to 0 (Phase-to-ground conductance per length) <br>
   * - R is greater 0 (Phase resistance per length) <br>
   * - X is greater 0 (Phase reactance per length) <br>
   * - iMax is greater 0 (Maximum permissible current) <br>
   * - vRated is greater 0 (Rated voltage)
   *
   * @param lineType Line type to validate
   * @return a list of try objects either containing an {@link InvalidEntityException} or an empty
   *     Success
   */
  protected static List<Try<Void, InvalidEntityException>> checkLineType(LineTypeInput lineType) {
    Try<Void, InvalidEntityException> isNull = checkNonNull(lineType, "a line type");

    if (isNull.isFailure()) {
      return List.of(isNull);
    }

    return Try.ofVoid(
        InvalidEntityException.class,
        () ->
            detectNegativeQuantities(
                new Quantity<?>[] {lineType.getB(), lineType.getG()}, lineType),
        () ->
            detectZeroOrNegativeQuantities(
                new Quantity<?>[] {
                  lineType.getvRated(), lineType.getiMax(), lineType.getX(), lineType.getR()
                },
                lineType));
  }

  /**
   * Validates a transformer2W if: <br>
   * - {@link ConnectorValidationUtils#checkTransformer2WType(Transformer2WTypeInput)} confirms a
   * valid type properties <br>
   * - its tap position is within bounds <br>
   * - it connects different subnets <br>
   * - it connects different voltage levels <br>
   * - its rated voltages match the voltages at the nodes
   *
   * @param transformer2W Transformer2W to validate
   * @return a list of try objects either containing an {@link InvalidEntityException} or an empty
   *     Success
   */
  private static List<Try<Void, InvalidEntityException>> checkTransformer2W(
      Transformer2WInput transformer2W) {
    List<Try<Void, InvalidEntityException>> exceptions =
        new ArrayList<>(checkTransformer2WType(transformer2W.getType()));

    exceptions.addAll(
        Try.ofVoid(
            InvalidEntityException.class,
            () -> checkIfTapPositionIsWithinBounds(transformer2W),
            () -> connectsNodesWithDifferentVoltageLevels(transformer2W, true),
            () -> connectsNodesToCorrectVoltageSides(transformer2W),
            () -> connectsNodesInDifferentSubnets(transformer2W, true),
            () -> ratedVoltageOfTransformer2WMatchesVoltagesOfNodes(transformer2W)));

    return exceptions;
  }

  /**
   * Validates a transformer2W type if: <br>
   * - it is not null <br>
   * - rSc is greater 0 (short circuit resistance) <br>
   * - xSc is greater 0 (short circuit impedance) <br>
   * - gM is greater/equal to 0 (no load conductance) <br>
   * - bM is less/equal to 0 (no load susceptance)<br>
   * - sRated is greater 0 (rated apparent power) <br>
   * - vRatedA is greater 0 (rated voltage at higher voltage terminal) <br>
   * - vRatedB is greater 0 (rated voltage at lower voltage terminal) <br>
   * - dV is between 0% and 100% (voltage magnitude increase per tap position <br>
   * - dPhi is greater/equal to 0 (voltage angle increase per tap position) <br>
   * - neutral tap position is between min and max tap position <br>
   * - minimum tap position is smaller than maximum tap position
   *
   * @param transformer2WType Transformer2W type to validate
   * @return a list of try objects either containing an {@link InvalidEntityException} or an empty
   *     Success
   */
  protected static List<Try<Void, InvalidEntityException>> checkTransformer2WType(
      Transformer2WTypeInput transformer2WType) {
    Try<Void, InvalidEntityException> isNull =
        checkNonNull(transformer2WType, "a two winding transformer type");

    if (isNull.isFailure()) {
      return List.of(isNull);
    }

    return Try.ofVoid(
        InvalidEntityException.class,
        () ->
            detectNegativeQuantities(
                new Quantity<?>[] {
                  transformer2WType.getgM(), transformer2WType.getdPhi(), transformer2WType.getrSc()
                },
                transformer2WType),
        () ->
            detectZeroOrNegativeQuantities(
                new Quantity<?>[] {
                  transformer2WType.getsRated(),
                  transformer2WType.getvRatedA(),
                  transformer2WType.getvRatedB(),
                  transformer2WType.getxSc()
                },
                transformer2WType),
        () ->
            detectPositiveQuantities(
                new Quantity<?>[] {transformer2WType.getbM()}, transformer2WType),
        () -> checkVoltageMagnitudeChangePerTapPosition(transformer2WType),
        () -> checkMinimumTapPositionIsLowerThanMaximumTapPosition(transformer2WType),
        () -> checkNeutralTapPositionLiesBetweenMinAndMaxTapPosition(transformer2WType));
  }

  /**
   * Validates a transformer3W if: <br>
   * - {@link ConnectorValidationUtils#checkTransformer3WType(Transformer3WTypeInput)} confirm a
   * valid type <br>
   * - its tap position is within bounds <br>
   * - it connects different subnets <br>
   * - it connects different voltage levels <br>
   * - its rated voltages match the voltages at the nodes
   *
   * @param transformer3W Transformer3W to validate
   * @return a list of try objects either containing an {@link InvalidEntityException} or an empty
   *     Success
   */
  private static List<Try<Void, InvalidEntityException>> checkTransformer3W(
      Transformer3WInput transformer3W) {
    List<Try<Void, InvalidEntityException>> exceptions =
        new ArrayList<>(checkTransformer3WType(transformer3W.getType()));

    exceptions.add(
        Try.ofVoid(
            () -> checkIfTapPositionIsWithinBounds(transformer3W), InvalidEntityException.class));

    // Check if transformer connects different voltage levels
    exceptions.add(
        Try.ofVoid(
            transformer3W.getNodeA().getVoltLvl() == transformer3W.getNodeB().getVoltLvl()
                || transformer3W.getNodeA().getVoltLvl() == transformer3W.getNodeC().getVoltLvl()
                || transformer3W.getNodeB().getVoltLvl() == transformer3W.getNodeC().getVoltLvl(),
            () ->
                new InvalidEntityException(
                    "Transformer connects nodes of the same voltage level", transformer3W)));

    // Check if transformer connects different subnets
    exceptions.add(
        Try.ofVoid(
            transformer3W.getNodeA().getSubnet() == transformer3W.getNodeB().getSubnet()
                || transformer3W.getNodeA().getSubnet() == transformer3W.getNodeC().getSubnet()
                || transformer3W.getNodeB().getSubnet() == transformer3W.getNodeC().getSubnet(),
            () ->
                new InvalidEntityException(
                    "Transformer connects nodes in the same subnet", transformer3W)));

    exceptions.addAll(
        Try.ofVoid(
            InvalidEntityException.class,
            () -> connectNodesToCorrectVoltageSides(transformer3W),
            () -> ratedVoltageOfTransformer3WMatchesVoltagesOfNodes(transformer3W)));

    return exceptions;
  }

  /**
   * Validates a transformer3W type if: <br>
   * - it is not null <br>
   * - rScA, rScB, rScC are greater 0 (short circuit resistance in branches A,B,C) <br>
   * - xScA, xScB, xScC are greater 0 (short circuit impedance in branches A,B,C) <br>
   * - gM is greater/equal to 0 (no load conductance) <br>
   * - bM is less/equal to 0 (no load susceptance) <br>
   * - sRatedA, sRatedB, sRatedC are greater 0 (rated apparent power in branches A,B,C) <br>
   * - vRatedA, vRatedB, vRatedC are greater 0 (rated voltage at higher node A,B,C) <br>
   * - dV is between 0% and 100% (voltage magnitude increase per tap position <br>
   * - dPhi is greater/equal to 0 (voltage angle increase per tap position) <br>
   * - neutral tap position is between min and max tap position <br>
   * - minimum tap position is smaller than maximum tap position <br>
   *
   * @param transformer3WType Transformer type to validate
   * @return a list of try objects either containing an {@link InvalidEntityException} or an empty
   *     Success
   */
  protected static List<Try<Void, InvalidEntityException>> checkTransformer3WType(
      Transformer3WTypeInput transformer3WType) {
    Try<Void, InvalidEntityException> isNull =
        checkNonNull(transformer3WType, "a three winding transformer type");

    if (isNull.isFailure()) {
      return List.of(isNull);
    }

    return Try.ofVoid(
        InvalidEntityException.class,
        () ->
            detectNegativeQuantities(
                new Quantity<?>[] {transformer3WType.getgM(), transformer3WType.getdPhi()},
                transformer3WType),
        () ->
            detectZeroOrNegativeQuantities(
                new Quantity<?>[] {
                  transformer3WType.getsRatedA(),
                  transformer3WType.getsRatedB(),
                  transformer3WType.getsRatedC(),
                  transformer3WType.getvRatedA(),
                  transformer3WType.getvRatedB(),
                  transformer3WType.getvRatedC(),
                  transformer3WType.getrScA(),
                  transformer3WType.getrScB(),
                  transformer3WType.getrScC(),
                  transformer3WType.getxScA(),
                  transformer3WType.getxScB(),
                  transformer3WType.getxScC()
                },
                transformer3WType),
        () ->
            detectPositiveQuantities(
                new Quantity<?>[] {transformer3WType.getbM()}, transformer3WType),
        () -> checkVoltageMagnitudeChangePerTapPosition(transformer3WType),
        () -> checkMinimumTapPositionIsLowerThanMaximumTapPosition(transformer3WType),
        () -> checkNeutralTapPositionLiesBetweenMinAndMaxTapPosition(transformer3WType));
  }

  /**
   * Validates a switch if: <br>
   * - its connected nodes are in the same voltage level
   *
   * @param switchInput Switch to validate
   * @return a try object either containing an {@link InvalidEntityException} or an empty Success
   */
  private static Try<Void, InvalidEntityException> checkSwitch(SwitchInput switchInput) {
    return Try.ofVoid(
        !switchInput.getNodeA().getVoltLvl().equals(switchInput.getNodeB().getVoltLvl()),
        () ->
            new InvalidEntityException(
                "Switch connects two different voltage levels", switchInput));
    /* Remark: Connecting two different "subnets" is fine, because as of our definition regarding a switchgear in
     * "upstream" direction of a transformer, all the nodes, that hare within the switch chain, belong to the lower
     * grid, whilst the "real" upper node is within the upper grid */
  }

  /**
   * Check if all given elements are connected.
   *
   * @param subGridContainer the subgrid to check the connectivity for
   * @return a try object either containing an {@link InvalidGridException} or an empty Success
   */
  protected static Try<Void, ValidationException> checkConnectivity(
      SubGridContainer subGridContainer) {
    Graph<UUID, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

    subGridContainer.getRawGrid().getNodes().forEach(node -> graph.addVertex(node.getUuid()));
    subGridContainer
        .getRawGrid()
        .getLines()
        .forEach(line -> graph.addEdge(line.getNodeA().getUuid(), line.getNodeB().getUuid()));
    subGridContainer
        .getRawGrid()
        .getTransformer2Ws()
        .forEach(
            trafo2w -> graph.addEdge(trafo2w.getNodeA().getUuid(), trafo2w.getNodeB().getUuid()));
    subGridContainer
        .getRawGrid()
        .getTransformer3Ws()
        .forEach(
            trafor3w -> {
              graph.addEdge(trafor3w.getNodeA().getUuid(), trafor3w.getNodeInternal().getUuid());
              graph.addEdge(trafor3w.getNodeInternal().getUuid(), trafor3w.getNodeB().getUuid());
              graph.addEdge(trafor3w.getNodeInternal().getUuid(), trafor3w.getNodeC().getUuid());
            });
    subGridContainer
        .getRawGrid()
        .getSwitches()
        .forEach(
            switches ->
                graph.addEdge(switches.getNodeA().getUuid(), switches.getNodeB().getUuid()));

    ConnectivityInspector<UUID, DefaultEdge> inspector = new ConnectivityInspector<>(graph);

    return Try.ofVoid(
        !inspector.isConnected(),
        () ->
            new InvalidGridException(
                "The grid with subnetNo "
                    + subGridContainer.getSubnet()
                    + " is not connected! Please ensure that all elements are connected correctly!"));
  }

  /**
   * Check that a connector connects different nodes
   *
   * @param connectorInput connectorInput to validate
   */
  private static Try<Void, InvalidEntityException> connectsDifferentNodes(
      ConnectorInput connectorInput) {
    return Try.ofVoid(
        connectorInput.getNodeA().equals(connectorInput.getNodeB()),
        () ->
            new InvalidEntityException(
                connectorInput.getClass().getSimpleName()
                    + " connects the same node, but shouldn't",
                connectorInput));
  }

  /**
   * Check if subnets of connector's nodes are correct depending on if they should be equal or not
   *
   * @param connectorInput ConnectorInput to validate
   * @param shouldBeDifferent determines if subnets should be equal or not
   */
  private static void connectsNodesInDifferentSubnets(
      ConnectorInput connectorInput, boolean shouldBeDifferent) throws InvalidEntityException {
    boolean isDifferent =
        connectorInput.getNodeA().getSubnet() != connectorInput.getNodeB().getSubnet();
    if (shouldBeDifferent && !isDifferent) {
      if (connectorInput.getNodeA().getSubnet() == connectorInput.getNodeB().getSubnet()) {
        throw new InvalidEntityException(
            connectorInput.getClass().getSimpleName() + " connects the same subnet, but shouldn't",
            connectorInput);
      }
    }
    if (!shouldBeDifferent && isDifferent) {
      throw new InvalidEntityException(
          connectorInput.getClass().getSimpleName() + " connects different subnets, but shouldn't",
          connectorInput);
    }
  }

  /**
   * Check if voltage levels of connector's nodes are correct depending on if they should be equal
   * or not
   *
   * @param connectorInput ConnectorInput to validate
   * @param shouldBeDifferent determines if voltage levels should be different or not
   */
  private static void connectsNodesWithDifferentVoltageLevels(
      ConnectorInput connectorInput, boolean shouldBeDifferent) throws InvalidEntityException {

    boolean vltLvlsAreEqual =
        connectorInput.getNodeA().getVoltLvl().equals(connectorInput.getNodeB().getVoltLvl());

    if (vltLvlsAreEqual && shouldBeDifferent) {
      throw new InvalidEntityException(
          connectorInput.getClass().getSimpleName()
              + " connects the same voltage level, but shouldn't",
          connectorInput);
    }

    if (!vltLvlsAreEqual && !shouldBeDifferent) {
      throw new InvalidEntityException(
          connectorInput.getClass().getSimpleName()
              + " connects different voltage levels, but shouldn't",
          connectorInput);
    }
  }

  /**
   * Check if voltage level of node a is higher than voltage level of node b.
   *
   * @param transformer Transformer2WInput to validate
   */
  private static void connectsNodesToCorrectVoltageSides(Transformer2WInput transformer)
      throws InvalidEntityException {
    try {
      connectsNodesToCorrectVoltageSides(transformer.getNodeA(), transformer.getNodeB());
    } catch (IllegalArgumentException e) {
      throw new InvalidEntityException(
          transformer.getClass().getSimpleName() + " has nodeA on the lower voltage side.",
          transformer);
    }
  }

  /**
   * Checks whether the voltage level of node a exceeds the voltage level of node b
   *
   * @param nodeA Node a
   * @param nodeB Node b
   */
  public static void connectsNodesToCorrectVoltageSides(NodeInput nodeA, NodeInput nodeB) {
    if (nodeB
        .getVoltLvl()
        .getNominalVoltage()
        .isGreaterThan(nodeA.getVoltLvl().getNominalVoltage())) {
      throw new IllegalArgumentException(
          "nodeA must be on the higher voltage side of the transformer");
    }
  }

  /**
   * Checks, if nodeA is greater than nodeB and nodeB is greater than nodeC of the {@link
   * Transformer3WInput}
   *
   * @param transformer Transformer3WInput to validate
   */
  private static void connectNodesToCorrectVoltageSides(Transformer3WInput transformer)
      throws InvalidEntityException {
    try {
      connectsNodesToCorrectVoltageSides(
          transformer.getNodeA(), transformer.getNodeB(), transformer.getNodeC());
    } catch (IllegalArgumentException e) {
      throw new InvalidEntityException(
          transformer.getClass().getSimpleName()
              + " has an invalid node voltage side configuration.",
          e,
          transformer);
    }
  }

  /**
   * Checks whether the voltage level sides are correctly assigned. (nodeA > nodeB > nodeC)
   *
   * @param nodeA NodeInput a of the transformer
   * @param nodeB NodeInput b of the transformer
   * @param nodeC NodeInput c of the transformer
   */
  public static void connectsNodesToCorrectVoltageSides(
      NodeInput nodeA, NodeInput nodeB, NodeInput nodeC) {
    String errorMessage =
        "Voltage level of node a must be greater than voltage level of node b and voltage level of node b must be greater than voltage level of node c";
    if (nodeC
        .getVoltLvl()
        .getNominalVoltage()
        .isGreaterThan(nodeB.getVoltLvl().getNominalVoltage())) {
      throw new IllegalArgumentException(errorMessage);
    }

    if (nodeB
        .getVoltLvl()
        .getNominalVoltage()
        .isGreaterThan(nodeA.getVoltLvl().getNominalVoltage())) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Check if the coordinates of the start and end points of a line equal the coordinates of the
   * nodes
   *
   * @param line LineInput to validate
   */
  private static void coordinatesOfLineEqualCoordinatesOfNodes(LineInput line) {
    if (!(line.getGeoPosition()
            .getStartPoint()
            .isWithinDistance(line.getNodeA().getGeoPosition(), ALLOWED_COORDINATE_ERROR)
        || line.getGeoPosition()
            .getEndPoint()
            .isWithinDistance(line.getNodeA().getGeoPosition(), ALLOWED_COORDINATE_ERROR)))
      logger.warn(
          "Coordinates of start and end point do not match coordinates of connected nodes: {}",
          line);
    if (!(line.getGeoPosition()
            .getStartPoint()
            .isWithinDistance(line.getNodeB().getGeoPosition(), ALLOWED_COORDINATE_ERROR)
        || line.getGeoPosition()
            .getEndPoint()
            .isWithinDistance(line.getNodeB().getGeoPosition(), ALLOWED_COORDINATE_ERROR)))
      logger.warn(
          "Coordinates of start and end point do not match coordinates of connected nodes: {}",
          line);
  }

  /**
   * Check if the line length matches the cumulated distances between the single points of the line
   *
   * @param line LineInput to validate
   */
  private static void lineLengthMatchesDistancesBetweenPointsOfLineString(LineInput line) {
    // only if not geo positions of both nodes are dummy values
    if ((line.getNodeA().getGeoPosition() != NodeInput.DEFAULT_GEO_POSITION
            || line.getNodeB().getGeoPosition() != NodeInput.DEFAULT_GEO_POSITION)
        && line.getLength()
            .isGreaterThan(
                GeoUtils.calcHaversine(line.getGeoPosition()).multiply(ALLOWED_LENGTH_ERROR))) {
      logger.warn(
          "Line length is more than {}% greater than the calculated distances between points building the line: {}",
          ALLOWED_LENGTH_ERROR, line);
    }
  }

  /**
   * Check if tap position is within bounds
   *
   * @param transformer2W Transformer2WInput to validate
   */
  private static void checkIfTapPositionIsWithinBounds(Transformer2WInput transformer2W)
      throws InvalidEntityException {
    if (transformer2W.getTapPos() < transformer2W.getType().getTapMin()
        || transformer2W.getTapPos() > transformer2W.getType().getTapMax())
      throw new InvalidEntityException(
          "Tap position of " + transformer2W.getClass().getSimpleName() + " is outside of bounds",
          transformer2W);
  }

  /**
   * Check if tap position is within bounds
   *
   * @param transformer3W Transformer3WInput to validate
   */
  private static void checkIfTapPositionIsWithinBounds(Transformer3WInput transformer3W)
      throws InvalidEntityException {
    if (transformer3W.getTapPos() < transformer3W.getType().getTapMin()
        || transformer3W.getTapPos() > transformer3W.getType().getTapMax())
      throw new InvalidEntityException(
          "Tap position of " + transformer3W.getClass().getSimpleName() + " is outside of bounds",
          transformer3W);
  }

  /**
   * Check if vRated of transformer match voltLvl of nodes
   *
   * @param transformer2W Transformer2WInput to validate
   */
  private static void ratedVoltageOfTransformer2WMatchesVoltagesOfNodes(
      Transformer2WInput transformer2W) throws InvalidEntityException {
    if (!QuantityUtil.isEquivalentAbs(
            transformer2W.getType().getvRatedA(),
            transformer2W.getNodeA().getVoltLvl().getNominalVoltage(),
            ALLOWED_VOLTAGE_ERROR)
        || !QuantityUtil.isEquivalentAbs(
            transformer2W.getType().getvRatedB(),
            transformer2W.getNodeB().getVoltLvl().getNominalVoltage(),
            ALLOWED_VOLTAGE_ERROR))
      throw new InvalidEntityException(
          "Rated voltages of "
              + transformer2W.getClass().getSimpleName()
              + " do not equal voltage levels at the nodes",
          transformer2W);
  }

  /**
   * Check if vRated of transformer match voltLvl of nodes
   *
   * @param transformer3W Transformer3WInput to validate
   */
  private static void ratedVoltageOfTransformer3WMatchesVoltagesOfNodes(
      Transformer3WInput transformer3W) throws InvalidEntityException {
    if (!QuantityUtil.isEquivalentAbs(
            transformer3W.getType().getvRatedA(),
            transformer3W.getNodeA().getVoltLvl().getNominalVoltage(),
            ALLOWED_VOLTAGE_ERROR)
        || !QuantityUtil.isEquivalentAbs(
            transformer3W.getType().getvRatedB(),
            transformer3W.getNodeB().getVoltLvl().getNominalVoltage(),
            ALLOWED_VOLTAGE_ERROR)
        || !QuantityUtil.isEquivalentAbs(
            transformer3W.getType().getvRatedC(),
            transformer3W.getNodeC().getVoltLvl().getNominalVoltage(),
            ALLOWED_VOLTAGE_ERROR))
      throw new InvalidEntityException(
          "Rated voltages of "
              + transformer3W.getClass().getSimpleName()
              + " do not equal voltage levels at the nodes",
          transformer3W);
  }

  /**
   * Check if voltage magnitude increase per tap position of transformer2WType is between 0% and
   * 100%
   *
   * @param transformer2WType Transformer2WTypeInput to validate
   */
  private static void checkVoltageMagnitudeChangePerTapPosition(
      Transformer2WTypeInput transformer2WType) throws InvalidEntityException {
    if (transformer2WType.getdV().isLessThan(Quantities.getQuantity(0d, Units.PERCENT))
        || transformer2WType.getdV().isGreaterThan(Quantities.getQuantity(100d, Units.PERCENT)))
      throw new InvalidEntityException(
          "Voltage magnitude increase per tap position must be between 0% and 100%",
          transformer2WType);
  }

  /**
   * Check if voltage magnitude increase per tap position of transformer3WType is between 0% and
   * 100%
   *
   * @param transformer3WType Transformer3WTypeInput to validate
   */
  private static void checkVoltageMagnitudeChangePerTapPosition(
      Transformer3WTypeInput transformer3WType) throws InvalidEntityException {
    if (transformer3WType.getdV().isLessThan(Quantities.getQuantity(0d, Units.PERCENT))
        || transformer3WType.getdV().isGreaterThan(Quantities.getQuantity(100d, Units.PERCENT)))
      throw new InvalidEntityException(
          "Voltage magnitude increase per tap position must be between 0% and 100%",
          transformer3WType);
  }

  /**
   * Check if minimum tap position is lower than maximum tap position
   *
   * @param transformer2WType Transformer2WTypeInput to validate
   */
  private static void checkMinimumTapPositionIsLowerThanMaximumTapPosition(
      Transformer2WTypeInput transformer2WType) throws InvalidEntityException {
    if (transformer2WType.getTapMax() < transformer2WType.getTapMin())
      throw new InvalidEntityException(
          "Minimum tap position must be lower than maximum tap position", transformer2WType);
  }

  /**
   * Check if minimum tap position is lower than maximum tap position
   *
   * @param transformer3WType Transformer3WTypeInput to validate
   */
  private static void checkMinimumTapPositionIsLowerThanMaximumTapPosition(
      Transformer3WTypeInput transformer3WType) throws InvalidEntityException {
    if (transformer3WType.getTapMax() < transformer3WType.getTapMin())
      throw new InvalidEntityException(
          "Minimum tap position must be lower than maximum tap position", transformer3WType);
  }

  /**
   * Check if neutral tap position lies between minimum and maximum tap position
   *
   * @param transformer2WType Transformer3WTypeInput to validate
   */
  private static void checkNeutralTapPositionLiesBetweenMinAndMaxTapPosition(
      Transformer2WTypeInput transformer2WType) throws InvalidEntityException {
    if (transformer2WType.getTapNeutr() < transformer2WType.getTapMin()
        || transformer2WType.getTapNeutr() > transformer2WType.getTapMax())
      throw new InvalidEntityException(
          "Neutral tap position must be between minimum and maximum tap position",
          transformer2WType);
  }

  /**
   * Check if neutral tap position lies between minimum and maximum tap position
   *
   * @param transformer3WType Transformer3WTypeInput to validate
   */
  private static void checkNeutralTapPositionLiesBetweenMinAndMaxTapPosition(
      Transformer3WTypeInput transformer3WType) throws InvalidEntityException {
    if (transformer3WType.getTapNeutr() < transformer3WType.getTapMin()
        || transformer3WType.getTapNeutr() > transformer3WType.getTapMax())
      throw new InvalidEntityException(
          "Neutral tap position must be between minimum and maximum tap position",
          transformer3WType);
  }
}
