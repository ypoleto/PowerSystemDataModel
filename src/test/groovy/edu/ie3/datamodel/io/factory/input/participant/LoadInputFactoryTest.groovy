/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */
package edu.ie3.datamodel.io.factory.input.participant

import edu.ie3.datamodel.models.input.system.characteristic.CharacteristicCoordinate
import tec.uom.se.quantity.Quantities

import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Power

import static edu.ie3.util.quantities.PowerSystemUnits.KILOWATT
import static edu.ie3.util.quantities.PowerSystemUnits.PU

import edu.ie3.datamodel.models.BdewLoadProfile
import edu.ie3.datamodel.models.OperationTime
import edu.ie3.datamodel.models.StandardUnits
import edu.ie3.datamodel.models.input.NodeInput
import edu.ie3.datamodel.models.input.OperatorInput
import edu.ie3.datamodel.models.input.system.LoadInput
import edu.ie3.test.helper.FactoryTestHelper
import spock.lang.Specification

class LoadInputFactoryTest extends Specification implements FactoryTestHelper {
	def "A LoadInputFactory should contain exactly the expected class for parsing"() {
		given:
		def inputFactory = new LoadInputFactory()
		def expectedClasses = [LoadInput]

		expect:
		inputFactory.classes() == Arrays.asList(expectedClasses.toArray())
	}

	def "A LoadInputFactory should parse a valid LoadInput correctly"() {
		given: "a system participant input type factory and model data"
		def inputFactory = new LoadInputFactory()
		Map<String, String> parameter = [
			"uuid"            : "91ec3bcf-1777-4d38-af67-0bf7c9fa73c7",
			"id"              : "TestID",
			"qcharacteristics": "cosPhiFixed:{(0.0,1.0)}",
			"slp"             : "G-4",
			"dsm"             : "true",
			"econsannual"     : "3",
			"srated"          : "4",
			"cosphi"          : "5"
		]
		def inputClass = LoadInput
		def nodeInput = Mock(NodeInput)

		when:
		Optional<LoadInput> input = inputFactory.getEntity(
				new SystemParticipantEntityData(parameter, inputClass, nodeInput))

		then:
		input.present
		input.get().getClass() == inputClass
		((LoadInput) input.get()).with {
			assert uuid == UUID.fromString(parameter["uuid"])
			assert operationTime == OperationTime.notLimited()
			assert operator == OperatorInput.NO_OPERATOR_ASSIGNED
			assert id == parameter["id"]
			assert node == nodeInput
			assert qCharacteristics.with {
				assert uuid != null
				assert coordinates == Collections.unmodifiableSortedSet([
					new CharacteristicCoordinate<Power, Dimensionless>(Quantities.getQuantity(0d, KILOWATT), Quantities.getQuantity(1d, PU))
				] as TreeSet)
			}
			assert standardLoadProfile == BdewLoadProfile.G4
			assert dsm
			assert eConsAnnual == getQuant(parameter["econsannual"], StandardUnits.ENERGY_IN)
			assert sRated == getQuant(parameter["srated"], StandardUnits.S_RATED)
			assert cosphiRated == Double.parseDouble(parameter["cosphi"])
		}
	}
}
