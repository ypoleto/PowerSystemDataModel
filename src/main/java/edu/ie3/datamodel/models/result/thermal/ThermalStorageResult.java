/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
*/
package edu.ie3.datamodel.models.result.thermal;

import edu.ie3.datamodel.models.StandardUnits;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Power;
import tec.uom.se.ComparableQuantity;

/**
 * Represents calculation results of {@link
 * edu.ie3.datamodel.models.input.thermal.ThermalStorageInput}
 */
public abstract class ThermalStorageResult extends ThermalUnitResult {
  /** Currently stored energy */
  private ComparableQuantity<Energy> energy; // TODO #65 Quantity replaced

  /**
   * Constructs the result with
   *
   * @param timestamp date and time when the result is produced
   * @param inputModel uuid of the input model that produces the result
   * @param energy Currently stored energy
   * @param qDot Heat power flowing into (> 0) or coming from (< 0) the storage
   */
  public ThermalStorageResult(
      ZonedDateTime timestamp,
      UUID inputModel,
      ComparableQuantity<Energy> energy, // TODO #65 Quantity replaced
      ComparableQuantity<Power> qDot) { // TODO #65 Quantity replaced
    super(timestamp, inputModel, qDot);
    this.energy = energy.to(StandardUnits.ENERGY_RESULT);
  }

  /**
   * Constructs the result with
   *
   * @param uuid uuid of this result entity, for automatic uuid generation use primary constructor
   *     above
   * @param timestamp date and time when the result is produced
   * @param inputModel uuid of the input model that produces the result
   * @param energy Currently stored energy
   * @param qDot Heat power flowing into (> 0) or coming from (< 0) the storage
   */
  public ThermalStorageResult(
      UUID uuid,
      ZonedDateTime timestamp,
      UUID inputModel,
      ComparableQuantity<Energy> energy,
      ComparableQuantity<Power> qDot) { // TODO #65 Quantity replaced
    super(uuid, timestamp, inputModel, qDot);
    this.energy = energy.to(StandardUnits.ENERGY_RESULT);
  }

  public ComparableQuantity<Energy> getEnergy() {
    return energy;
  } // TODO #65 Quantity replaced

  public void setEnergy(ComparableQuantity<Energy> energy) {
    this.energy = energy.to(StandardUnits.ENERGY_RESULT);
  } // TODO #65 Quantity replaced

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ThermalStorageResult that = (ThermalStorageResult) o;
    return energy.equals(that.energy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), energy);
  }

  @Override
  public String toString() {
    return "ThermalStorageResult{" + "energy=" + energy + '}';
  }
}
