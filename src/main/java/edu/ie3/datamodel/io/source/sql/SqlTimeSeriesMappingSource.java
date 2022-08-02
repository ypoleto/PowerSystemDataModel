/*
 * © 2021. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
*/
package edu.ie3.datamodel.io.source.sql;

import edu.ie3.datamodel.io.connectors.SqlConnector;
import edu.ie3.datamodel.io.factory.SimpleEntityData;
import edu.ie3.datamodel.io.factory.timeseries.TimeSeriesMappingFactory;
import edu.ie3.datamodel.io.naming.EntityPersistenceNamingStrategy;
import edu.ie3.datamodel.io.source.TimeSeriesMappingSource;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SqlTimeSeriesMappingSource
    extends SqlDataSource<SqlTimeSeriesMappingSource.MappingEntry>
    implements TimeSeriesMappingSource {
  private static final TimeSeriesMappingFactory mappingFactory = new TimeSeriesMappingFactory();

  private final EntityPersistenceNamingStrategy entityPersistenceNamingStrategy;
  private final String queryFull;
  private final String schemaName;

  public SqlTimeSeriesMappingSource(
      SqlConnector connector,
      String schemaName,
      EntityPersistenceNamingStrategy entityPersistenceNamingStrategy) {
    super(connector);
    this.entityPersistenceNamingStrategy = entityPersistenceNamingStrategy;

    final String tableName =
        entityPersistenceNamingStrategy.getEntityName(MappingEntry.class).orElseThrow();
    this.queryFull = createBaseQueryString(schemaName, tableName);

    this.schemaName = schemaName;
  }

  @Override
  public Map<UUID, UUID> getMapping() {
    return executeQuery(queryFull, ps -> {}).stream()
        .collect(Collectors.toMap(MappingEntry::getParticipant, MappingEntry::getTimeSeries));
  }

  /**
   * @deprecated since 3.0. Use {@link
   *     SqlTimeSeriesMetaInformationSource#getTimeSeriesMetaInformation()} instead
   */
  @Override
  @Deprecated(since = "3.0", forRemoval = true)
  public Optional<edu.ie3.datamodel.io.csv.timeseries.IndividualTimeSeriesMetaInformation>
      getTimeSeriesMetaInformation(UUID timeSeriesUuid) {
    return getDbTables(schemaName, "%" + timeSeriesUuid.toString()).stream()
        .findFirst()
        .map(entityPersistenceNamingStrategy::extractIndividualTimesSeriesMetaInformation);
  }

  @Override
  protected Optional<MappingEntry> createEntity(Map<String, String> fieldToValues) {
    SimpleEntityData entityData = new SimpleEntityData(fieldToValues, MappingEntry.class);
    return mappingFactory.get(entityData);
  }
}
