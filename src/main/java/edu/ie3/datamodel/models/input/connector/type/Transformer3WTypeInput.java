/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
*/
package edu.ie3.datamodel.models.input.connector.type;

import edu.ie3.datamodel.models.StandardUnits;
import edu.ie3.datamodel.models.input.AssetTypeInput;
import java.util.Objects;
import java.util.UUID;
import javax.measure.quantity.*;
import tec.uom.se.ComparableQuantity;

/** Describes the type of a {@link edu.ie3.datamodel.models.input.connector.Transformer3WInput} */
public class Transformer3WTypeInput extends AssetTypeInput {
  /** Rated apparent power of the high voltage winding (typically in MVA) */
  private final ComparableQuantity<Power> sRatedA; // Hv // TODO #65 Quantity replaced
  /** Rated apparent power of the medium voltage winding (typically in MVA) */
  private final ComparableQuantity<Power> sRatedB; // Mv // TODO #65 Quantity replaced
  /** Rated apparent power of the low voltage windings (typically in MVA) */
  private final ComparableQuantity<Power> sRatedC; // Lv // TODO #65 Quantity replaced
  /** Rated voltage magnitude of the high voltage winding (typically in kV) */
  private final ComparableQuantity<ElectricPotential> vRatedA; // Hv // TODO #65 Quantity replaced
  /** Rated voltage magnitude of the medium voltage winding (typically in kV) */
  private final ComparableQuantity<ElectricPotential> vRatedB; // Mv // TODO #65 Quantity replaced
  /** Rated voltage magnitude of the low voltage winding (typically in kV) */
  private final ComparableQuantity<ElectricPotential> vRatedC; // Lv // TODO #65 Quantity replaced
  /** Short-circuit resistance of the high voltage winding (typically in Ohm) */
  private final ComparableQuantity<ElectricResistance> rScA; // Hv // TODO #65 Quantity replaced
  /** Short-circuit resistance of the medium voltage winding (typically in Ohm) */
  private final ComparableQuantity<ElectricResistance> rScB; // Mv // TODO #65 Quantity replaced
  /** Short-circuit resistance of the low voltage winding (typically in Ohm) */
  private final ComparableQuantity<ElectricResistance> rScC; // Lv // TODO #65 Quantity replaced
  /** Short-circuit reactance of the high voltage winding (typically in Ohm) */
  private final ComparableQuantity<ElectricResistance> xScA; // Hv // TODO #65 Quantity replaced
  /** Short-circuit reactance of the medium voltage winding (typically in Ohm) */
  private final ComparableQuantity<ElectricResistance> xScB; // Mv // TODO #65 Quantity replaced
  /** Short-circuit reactance of the low voltage winding (typically in Ohm) */
  private final ComparableQuantity<ElectricResistance> xScC; // Lv // TODO #65 Quantity replaced
  /** Phase-to-ground conductance (typically in nS) */
  private final ComparableQuantity<ElectricConductance> gM; // TODO #65 Quantity replaced
  /** Phase-to-ground susceptance (typically in nS) */
  private final ComparableQuantity<ElectricConductance> bM; // TODO #65 Quantity replaced
  /** Voltage magnitude deviation per tap position (typically in %) */
  private final ComparableQuantity<Dimensionless> dV; // TODO #65 Quantity replaced
  /** Voltage angle deviation per tap position (typically in °) */
  private final ComparableQuantity<Angle> dPhi; // TODO #65 Quantity replaced
  /** Neutral tap position */
  private final int tapNeutr;
  /** Minimum available tap position */
  private final int tapMin;
  /** Maximum available tap position */
  private final int tapMax;

  /**
   * @param uuid of the input entity
   * @param id of this type
   * @param sRatedA Rated apparent power of the high voltage winding
   * @param sRatedB Rated apparent power of the medium voltage winding
   * @param sRatedC Rated apparent power of the low voltage winding
   * @param vRatedA Rated voltage magnitude of the high voltage winding
   * @param vRatedB Rated voltage magnitude of the medium voltage winding
   * @param vRatedC Rated voltage magnitude of the low voltage winding
   * @param rScA Short-circuit resistance of the high voltage winding
   * @param rScB Short-circuit resistance of the medium voltage winding
   * @param rScC Short-circuit resistance of the low voltage winding
   * @param xScA Short-circuit reactance of the high voltage winding
   * @param xScB Short-circuit reactance of the medium voltage winding
   * @param xScC Short-circuit reactance of the low voltage winding
   * @param gM Phase-to-ground conductance
   * @param bM Phase-to-ground susceptance
   * @param dV Voltage magnitude deviation per tap position
   * @param dPhi Voltage angle deviation per tap position
   * @param tapNeutr Neutral tap position
   * @param tapMin Minimum available tap position
   * @param tapMax Maximum available tap position
   */
  public Transformer3WTypeInput(
      UUID uuid,
      String id,
      ComparableQuantity<Power> sRatedA, // TODO #65 Quantity replaced
      ComparableQuantity<Power> sRatedB, // TODO #65 Quantity replaced
      ComparableQuantity<Power> sRatedC, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricPotential> vRatedA, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricPotential> vRatedB, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricPotential> vRatedC, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricResistance> rScA, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricResistance> rScB, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricResistance> rScC, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricResistance> xScA, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricResistance> xScB, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricResistance> xScC, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricConductance> gM, // TODO #65 Quantity replaced
      ComparableQuantity<ElectricConductance> bM, // TODO #65 Quantity replaced
      ComparableQuantity<Dimensionless> dV, // TODO #65 Quantity replaced
      ComparableQuantity<Angle> dPhi, // TODO #65 Quantity replaced
      int tapNeutr,
      int tapMin,
      int tapMax) {
    super(uuid, id);
    this.sRatedA = sRatedA.to(StandardUnits.S_RATED);
    this.sRatedB = sRatedB.to(StandardUnits.S_RATED);
    this.sRatedC = sRatedC.to(StandardUnits.S_RATED);
    this.vRatedA = vRatedA.to(StandardUnits.RATED_VOLTAGE_MAGNITUDE);
    this.vRatedB = vRatedB.to(StandardUnits.RATED_VOLTAGE_MAGNITUDE);
    this.vRatedC = vRatedC.to(StandardUnits.RATED_VOLTAGE_MAGNITUDE);
    this.rScA = rScA.to(StandardUnits.IMPEDANCE);
    this.rScB = rScB.to(StandardUnits.IMPEDANCE);
    this.rScC = rScC.to(StandardUnits.IMPEDANCE);
    this.xScA = xScA.to(StandardUnits.IMPEDANCE);
    this.xScB = xScB.to(StandardUnits.IMPEDANCE);
    this.xScC = xScC.to(StandardUnits.IMPEDANCE);
    this.gM = gM.to(StandardUnits.ADMITTANCE);
    this.bM = bM.to(StandardUnits.ADMITTANCE);
    this.dV = dV.to(StandardUnits.DV_TAP);
    this.dPhi = dPhi.to(StandardUnits.DPHI_TAP);
    this.tapNeutr = tapNeutr;
    this.tapMin = tapMin;
    this.tapMax = tapMax;
  }

  public ComparableQuantity<Power> getsRatedA() {
    return sRatedA;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<Power> getsRatedB() {
    return sRatedB;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<Power> getsRatedC() {
    return sRatedC;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricPotential> getvRatedA() {
    return vRatedA;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricPotential> getvRatedB() {
    return vRatedB;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricPotential> getvRatedC() {
    return vRatedC;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricResistance> getrScA() {
    return rScA;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricResistance> getrScB() {
    return rScB;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricResistance> getrScC() {
    return rScC;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricResistance> getxScA() {
    return xScA;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricResistance> getxScB() {
    return xScB;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricResistance> getxScC() {
    return xScC;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricConductance> getgM() {
    return gM;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<ElectricConductance> getbM() {
    return bM;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<Dimensionless> getdV() {
    return dV;
  } // TODO #65 Quantity replaced

  public ComparableQuantity<Angle> getdPhi() {
    return dPhi;
  } // TODO #65 Quantity replaced

  public int getTapNeutr() {
    return tapNeutr;
  }

  public int getTapMin() {
    return tapMin;
  }

  public int getTapMax() {
    return tapMax;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Transformer3WTypeInput that = (Transformer3WTypeInput) o;
    return tapNeutr == that.tapNeutr
        && tapMin == that.tapMin
        && tapMax == that.tapMax
        && sRatedA.equals(that.sRatedA)
        && sRatedB.equals(that.sRatedB)
        && sRatedC.equals(that.sRatedC)
        && vRatedA.equals(that.vRatedA)
        && vRatedB.equals(that.vRatedB)
        && vRatedC.equals(that.vRatedC)
        && rScA.equals(that.rScA)
        && rScB.equals(that.rScB)
        && rScC.equals(that.rScC)
        && xScA.equals(that.xScA)
        && xScB.equals(that.xScB)
        && xScC.equals(that.xScC)
        && gM.equals(that.gM)
        && bM.equals(that.bM)
        && dV.equals(that.dV)
        && dPhi.equals(that.dPhi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        sRatedA,
        sRatedB,
        sRatedC,
        vRatedA,
        vRatedB,
        vRatedC,
        rScA,
        rScB,
        rScC,
        xScA,
        xScB,
        xScC,
        gM,
        bM,
        dV,
        dPhi,
        tapNeutr,
        tapMin,
        tapMax);
  }

  @Override
  public String toString() {
    return "Transformer3WTypeInput{"
        + "sRatedA="
        + sRatedA
        + ", sRatedB="
        + sRatedB
        + ", sRatedC="
        + sRatedC
        + ", vRatedA="
        + vRatedA
        + ", vRatedB="
        + vRatedB
        + ", vRatedC="
        + vRatedC
        + ", rScA="
        + rScA
        + ", rScB="
        + rScB
        + ", rScC="
        + rScC
        + ", xScA="
        + xScA
        + ", xScB="
        + xScB
        + ", xScC="
        + xScC
        + ", gM="
        + gM
        + ", bM="
        + bM
        + ", dV="
        + dV
        + ", dPhi="
        + dPhi
        + ", tapNeutr="
        + tapNeutr
        + ", tapMin="
        + tapMin
        + ", tapMax="
        + tapMax
        + '}';
  }
}
