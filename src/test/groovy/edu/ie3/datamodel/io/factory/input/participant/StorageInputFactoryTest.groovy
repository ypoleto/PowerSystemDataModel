/*
 * © 2021. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */
package edu.ie3.datamodel.io.factory.input.participant

import edu.ie3.datamodel.exceptions.FactoryException
import edu.ie3.datamodel.models.input.NodeInput
import edu.ie3.datamodel.models.input.OperatorInput
import edu.ie3.datamodel.models.input.system.StorageInput
import edu.ie3.datamodel.models.input.system.characteristic.CharacteristicPoint
import edu.ie3.datamodel.models.input.system.type.StorageTypeInput
import edu.ie3.datamodel.utils.Try
import edu.ie3.test.helper.FactoryTestHelper
import spock.lang.Specification
import tech.units.indriya.quantity.Quantities

import javax.measure.quantity.Dimensionless
import java.time.ZonedDateTime

import static edu.ie3.util.quantities.PowerSystemUnits.PU

class StorageInputFactoryTest extends Specification implements FactoryTestHelper {
  def "A StorageInputFactory should contain exactly the expected class for parsing"() {
    given:
    def inputFactory = new StorageInputFactory()
    def expectedClasses = [StorageInput]

    expect:
    inputFactory.supportedClasses == Arrays.asList(expectedClasses.toArray())
  }

  def "A StorageInputFactory should parse a valid StorageInput correctly"() {
    given: "a system participant input type factory and model data"
    def inputFactory = new StorageInputFactory()
    Map<String, String> parameter = [
      "uuid"            : "91ec3bcf-1777-4d38-af67-0bf7c9fa73c7",
      "operatesfrom"    : "2019-01-01T00:00:00+01:00[Europe/Berlin]",
      "operatesuntil"   : "2019-12-31T23:59:00+01:00[Europe/Berlin]",
      "id"              : "TestID",
      "qcharacteristics": "cosPhiFixed:{(0.0,1.0)}"
    ]
    def inputClass = StorageInput
    def nodeInput = Mock(NodeInput)
    def operatorInput = Mock(OperatorInput)
    def typeInput = Mock(StorageTypeInput)

    when:
    Try<StorageInput, FactoryException> input = inputFactory.get(
        new SystemParticipantTypedEntityData<StorageTypeInput>(parameter, inputClass, operatorInput, nodeInput, typeInput))

    then:
    input.success
    input.data.get().getClass() == inputClass
    input.data.get().with {
      assert uuid == UUID.fromString(parameter["uuid"])
      assert operationTime.startDate.present
      assert operationTime.startDate.get() == ZonedDateTime.parse(parameter["operatesfrom"])
      assert operationTime.endDate.present
      assert operationTime.endDate.get() == ZonedDateTime.parse(parameter["operatesuntil"])
      assert operator == operatorInput
      assert id == parameter["id"]
      assert node == nodeInput
      assert qCharacteristics.with {
        assert uuid != null
        assert points == Collections.unmodifiableSortedSet([
          new CharacteristicPoint<Dimensionless, Dimensionless>(Quantities.getQuantity(0d, PU), Quantities.getQuantity(1d, PU))
        ] as TreeSet)
      }
      assert type == typeInput
    }
  }
}
