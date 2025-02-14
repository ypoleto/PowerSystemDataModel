/*
 * © 2021. TU Dortmund University,
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
import tech.units.indriya.ComparableQuantity;

/**
 * Represents calculation results of {@link
 * edu.ie3.datamodel.models.input.thermal.ThermalStorageInput}
 */
public abstract class ThermalStorageResult extends ThermalUnitResult {
  /** Currently stored energy */
  private ComparableQuantity<Energy> energy;

  /**
   * Constructs the result with
   *
   * @param time date and time when the result is produced
   * @param inputModel uuid of the input model that produces the result
   * @param energy Currently stored energy
   * @param qDot Heat power flowing into (&gt; 0) or coming from (&lt; 0) the storage
   */
  protected ThermalStorageResult(
      ZonedDateTime time,
      UUID inputModel,
      ComparableQuantity<Energy> energy,
      ComparableQuantity<Power> qDot) {
    super(time, inputModel, qDot);
    this.energy = energy.to(StandardUnits.ENERGY_RESULT);
  }

  /**
   * Constructs the result with
   *
   * @param uuid uuid of this result entity, for automatic uuid generation use primary constructor
   *     above
   * @param time date and time when the result is produced
   * @param inputModel uuid of the input model that produces the result
   * @param energy Currently stored energy
   * @param qDot Heat power flowing into (&gt; 0) or coming from (&lt; 0) the storage
   */
  protected ThermalStorageResult(
      UUID uuid,
      ZonedDateTime time,
      UUID inputModel,
      ComparableQuantity<Energy> energy,
      ComparableQuantity<Power> qDot) {
    super(uuid, time, inputModel, qDot);
    this.energy = energy.to(StandardUnits.ENERGY_RESULT);
  }

  public ComparableQuantity<Energy> getEnergy() {
    return energy;
  }

  public void setEnergy(ComparableQuantity<Energy> energy) {
    this.energy = energy.to(StandardUnits.ENERGY_RESULT);
  }

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
    return "ThermalStorageResult{"
        + "uuid="
        + getUuid()
        + ", time="
        + getTime()
        + ", inputModel="
        + getInputModel()
        + ", qDot="
        + getqDot()
        + ", energy="
        + energy
        + '}';
  }
}
