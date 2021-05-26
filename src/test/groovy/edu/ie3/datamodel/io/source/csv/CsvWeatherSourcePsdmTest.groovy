/*
 * © 2021. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */
package edu.ie3.datamodel.io.source.csv

import edu.ie3.datamodel.io.naming.EntityNamingStrategy

import static edu.ie3.datamodel.models.StandardUnits.SOLAR_IRRADIANCE
import static edu.ie3.datamodel.models.StandardUnits.TEMPERATURE
import static edu.ie3.datamodel.models.StandardUnits.WIND_DIRECTION
import static edu.ie3.datamodel.models.StandardUnits.WIND_VELOCITY

import edu.ie3.datamodel.io.factory.timeseries.PsdmTimeBasedWeatherValueFactory
import edu.ie3.datamodel.models.value.SolarIrradianceValue
import edu.ie3.datamodel.io.source.IdCoordinateSource
import edu.ie3.datamodel.models.timeseries.individual.IndividualTimeSeries
import edu.ie3.datamodel.models.timeseries.individual.TimeBasedValue
import edu.ie3.datamodel.models.value.TemperatureValue
import edu.ie3.datamodel.models.value.WeatherValue
import edu.ie3.datamodel.models.value.WindValue
import edu.ie3.test.common.PsdmWeatherTestData
import edu.ie3.test.helper.WeatherSourceTestHelper
import edu.ie3.util.TimeUtil
import edu.ie3.util.geo.GeoUtils
import edu.ie3.util.interval.ClosedInterval
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point
import spock.lang.Shared
import spock.lang.Specification
import tech.units.indriya.quantity.Quantities

class CsvWeatherSourcePsdmTest extends Specification implements CsvTestDataMeta, WeatherSourceTestHelper {

	@Shared
	CsvWeatherSource source

	@Shared
	IdCoordinateSource coordinateSource

	def setupSpec() {
		coordinateSource = PsdmWeatherTestData.coordinateSource
		def weatherFactory = new PsdmTimeBasedWeatherValueFactory()
		source = new CsvWeatherSource(";", timeSeriesFolderPath, new EntityNamingStrategy(), coordinateSource, weatherFactory)
	}

	def "A CsvWeatherSource can read and correctly parse a single value for a specific date and coordinate"() {
		given:
		def expectedTimeBasedValue = new TimeBasedValue(PsdmWeatherTestData.TIME_15H, PsdmWeatherTestData.WEATHER_VALUE_193186_15H)
		when:
		def optTimeBasedValue = source.getWeather(PsdmWeatherTestData.TIME_15H, PsdmWeatherTestData.COORDINATE_193186)
		then:
		optTimeBasedValue.present
		equalsIgnoreUUID(optTimeBasedValue.get(), expectedTimeBasedValue)
	}

	def "A CsvWeatherSource can read multiple time series values for multiple coordinates"() {
		given:
		def coordinates = [
			PsdmWeatherTestData.COORDINATE_193186,
			PsdmWeatherTestData.COORDINATE_193187
		]
		def timeInterval = new ClosedInterval(PsdmWeatherTestData.TIME_16H, PsdmWeatherTestData.TIME_17H)
		def timeSeries193186 = new IndividualTimeSeries(null,
				[
					new TimeBasedValue(PsdmWeatherTestData.TIME_16H, PsdmWeatherTestData.WEATHER_VALUE_193186_16H),
					new TimeBasedValue(PsdmWeatherTestData.TIME_17H, PsdmWeatherTestData.WEATHER_VALUE_193186_17H)]
				as Set<TimeBasedValue>)
		def timeSeries193187 = new IndividualTimeSeries(null,
				[
					new TimeBasedValue(PsdmWeatherTestData.TIME_16H, PsdmWeatherTestData.WEATHER_VALUE_193187_16H)] as Set<TimeBasedValue>)
		when:
		Map<Point, IndividualTimeSeries<WeatherValue>> coordinateToTimeSeries = source.getWeather(timeInterval, coordinates)
		then:
		coordinateToTimeSeries.keySet().size() == 2
		equalsIgnoreUUID(coordinateToTimeSeries.get(PsdmWeatherTestData.COORDINATE_193186), timeSeries193186)
		equalsIgnoreUUID(coordinateToTimeSeries.get(PsdmWeatherTestData.COORDINATE_193187), timeSeries193187)
	}


	def "A CsvWeatherSource can read all weather data in a given time interval"() {
		given:
		def timeInterval = new ClosedInterval(PsdmWeatherTestData.TIME_15H, PsdmWeatherTestData.TIME_17H)
		def timeSeries193186 = new IndividualTimeSeries(null,
				[
					new TimeBasedValue(PsdmWeatherTestData.TIME_15H, PsdmWeatherTestData.WEATHER_VALUE_193186_15H),
					new TimeBasedValue(PsdmWeatherTestData.TIME_16H, PsdmWeatherTestData.WEATHER_VALUE_193186_16H),
					new TimeBasedValue(PsdmWeatherTestData.TIME_17H, PsdmWeatherTestData.WEATHER_VALUE_193186_17H)] as Set<TimeBasedValue>)
		def timeSeries193187 = new IndividualTimeSeries(null,
				[
					new TimeBasedValue(PsdmWeatherTestData.TIME_15H, PsdmWeatherTestData.WEATHER_VALUE_193187_15H),
					new TimeBasedValue(PsdmWeatherTestData.TIME_16H, PsdmWeatherTestData.WEATHER_VALUE_193187_16H)] as Set<TimeBasedValue>)
		def timeSeries193188 = new IndividualTimeSeries(null,
				[
					new TimeBasedValue(PsdmWeatherTestData.TIME_15H, PsdmWeatherTestData.WEATHER_VALUE_193188_15H)] as Set<TimeBasedValue>)
		when:
		Map<Point, IndividualTimeSeries<WeatherValue>> coordinateToTimeSeries = source.getWeather(timeInterval)
		then:
		coordinateToTimeSeries.keySet().size() == 3
		equalsIgnoreUUID(coordinateToTimeSeries.get(PsdmWeatherTestData.COORDINATE_193186).entries, timeSeries193186.entries)
		equalsIgnoreUUID(coordinateToTimeSeries.get(PsdmWeatherTestData.COORDINATE_193187).entries, timeSeries193187.entries)
		equalsIgnoreUUID(coordinateToTimeSeries.get(PsdmWeatherTestData.COORDINATE_193188).entries, timeSeries193188.entries)
	}

	def "The CsvWeatherSource is able to build a single WeatherValue from field to value mapping"() {
		given:
		def defaultCoordinate = GeoUtils.DEFAULT_GEOMETRY_FACTORY.createPoint(new Coordinate(7.4116482, 51.4843281))
		def coordinateSource = Mock(IdCoordinateSource)
		coordinateSource.getCoordinate(_) >> { args -> args[0] == 5 ? Optional.of(defaultCoordinate) : Optional.empty() }
		def weatherFactory = new PsdmTimeBasedWeatherValueFactory()
		def source = new CsvWeatherSource(";", timeSeriesFolderPath, new EntityNamingStrategy(), coordinateSource, weatherFactory)
		def fieldToValues = [
			"uuid"              : "71a79f59-eebf-40c1-8358-ba7414077d57",
			"time"              : "2020-10-16T12:40:42Z",
			"coordinate"        : "5",
			"directirradiation" : "1.234",
			"diffuseirradiation": "5.678",
			"temperature"       : "9.1011",
			"windvelocity"      : "12.1314",
			"winddirection"     : "15.1617"
		]
		def expectedValue = new TimeBasedValue(
				UUID.fromString("71a79f59-eebf-40c1-8358-ba7414077d57"),
				TimeUtil.withDefaults.toZonedDateTime("2020-10-16 12:40:42"),
				new WeatherValue(
				defaultCoordinate,
				new SolarIrradianceValue(
				Quantities.getQuantity(1.234, SOLAR_IRRADIANCE),
				Quantities.getQuantity(5.678, SOLAR_IRRADIANCE)
				),
				new TemperatureValue(
				Quantities.getQuantity(9.1011, TEMPERATURE)
				),
				new WindValue(
				Quantities.getQuantity(12.1314, WIND_DIRECTION),
				Quantities.getQuantity(15.1617, WIND_VELOCITY)
				)
				)
				)

		when:
		def actual = source.buildWeatherValue(fieldToValues)

		then:
		actual.present
		actual.get() == expectedValue
	}

	def "The CsvWeatherSource returns no WeatherValue, if the coordinate field is empty"() {
		given:
		def defaultCoordinate = GeoUtils.DEFAULT_GEOMETRY_FACTORY.createPoint(new Coordinate(7.4116482, 51.4843281))
		def coordinateSource = Mock(IdCoordinateSource)
		coordinateSource.getCoordinate(_) >> { args -> args[0] == 5 ? Optional.of(defaultCoordinate) : Optional.empty() }
		def weatherFactory = new PsdmTimeBasedWeatherValueFactory()
		def source = new CsvWeatherSource(";", timeSeriesFolderPath, new EntityNamingStrategy(), coordinateSource, weatherFactory)
		def fieldToValues = [
			"uuid"              : "71a79f59-eebf-40c1-8358-ba7414077d57",
			"time"              : "2020-10-16T12:40:42Z",
			"coordinate"        : "",
			"directirradiation" : "1.234",
			"diffuseirradiation": "5.678",
			"temperature"       : "9.1011",
			"windvelocity"      : "12.1314",
			"winddirection"     : "15.1617"
		]

		when:
		def actual = source.buildWeatherValue(fieldToValues)

		then:
		!actual.present
	}

	def "The CsvWeatherSource returns no WeatherValue, if the coordinate field is missing"() {
		given:
		def defaultCoordinate = GeoUtils.DEFAULT_GEOMETRY_FACTORY.createPoint(new Coordinate(7.4116482, 51.4843281))
		def coordinateSource = Mock(IdCoordinateSource)
		coordinateSource.getCoordinate(_) >> { args -> args[0] == 5 ? Optional.of(defaultCoordinate) : Optional.empty() }
		def weatherFactory = new PsdmTimeBasedWeatherValueFactory()
		def source = new CsvWeatherSource(";", timeSeriesFolderPath, new EntityNamingStrategy(), coordinateSource, weatherFactory)
		def fieldToValues = [
			"uuid"              : "71a79f59-eebf-40c1-8358-ba7414077d57",
			"time"              : "2020-10-16T12:40:42Z",
			"directirradiation" : "1.234",
			"diffuseirradiation": "5.678",
			"temperature"       : "9.1011",
			"windvelocity"      : "12.1314",
			"winddirection"     : "15.1617"
		]

		when:
		def actual = source.buildWeatherValue(fieldToValues)

		then:
		!actual.present
	}

	def "The CsvWeatherSource returns no WeatherValue, if the coordinate cannot be obtained"() {
		given:
		def coordinateSource = Mock(IdCoordinateSource)
		coordinateSource.getCoordinate(_) >> Optional.empty()
		def weatherFactory = new PsdmTimeBasedWeatherValueFactory()
		def source = new CsvWeatherSource(";", timeSeriesFolderPath, new EntityNamingStrategy(), coordinateSource, weatherFactory)
		def fieldToValues = [
			"uuid"              : "71a79f59-eebf-40c1-8358-ba7414077d57",
			"time"              : "2020-10-16T12:40:42Z",
			"coordinate"        : "6",
			"directirradiation" : "1.234",
			"diffuseirradiation": "5.678",
			"temperature"       : "9.1011",
			"windvelocity"      : "12.1314",
			"winddirection"     : "15.1617"
		]

		when:
		def actual = source.buildWeatherValue(fieldToValues)

		then:
		!actual.present
	}
}
