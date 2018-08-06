package biocode.fims.photos.reader;

import biocode.fims.application.config.PhotosSql;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.GenericRecordRowMapper;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.PostgresUtils;
import biocode.fims.reader.DataConverter;
import biocode.fims.repositories.RecordRepository;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class PhotoConverter implements DataConverter {
    private final PhotosSql photosSql;
    private final RecordRepository recordRepository;
    protected File file;
    protected ProjectConfig config;

    public PhotoConverter(PhotosSql photosSql, RecordRepository recordRepository) {
        this.photosSql = photosSql;
        this.recordRepository = recordRepository;
    }

    public PhotoConverter(PhotosSql photosSql, RecordRepository recordRepository, ProjectConfig projectConfig) {
        this(photosSql, recordRepository);
        this.config = projectConfig;
    }

    @Override
    public RecordSet convertRecordSet(RecordSet recordSet, int projectId, String expeditionCode) {
        String parent = recordSet.entity().getParentEntity();
        String parentKey = config.entity(parent).getUniqueKeyURI();

        List<PhotoRecord> existingRecords = getExistingRecords(recordSet, projectId, expeditionCode, parentKey);
        updateRecords(recordSet, existingRecords, parentKey);
        return recordSet;
    }

    /**
     * Preserve processing data if the record already exists.
     * <p>
     * This is necessary because we do additional processing on photos and need to preserve some data.
     *
     * @param recordSet
     * @param existingRecords
     * @param parentKey
     */
    private void updateRecords(RecordSet recordSet, List<PhotoRecord> existingRecords, String parentKey) {
        for (Record r : recordSet.recordsToPersist()) {
            PhotoRecord record = (PhotoRecord) r;

            record.set(PhotoEntityProps.PROCESSED.value(), "false");

            existingRecords.stream()
                    .filter(er -> er.photoID().equals(record.photoID()) && er.get(parentKey).equals(record.get(parentKey)))
                    .findFirst()
                    .ifPresent(er -> {
                        // if the originalUrl is the same copy a few existing props
                        // TODO possibly need to persist more data?
                        if (er.originalUrl().equals(record.originalUrl())) {
                            for (PhotoEntityProps p : PhotoEntityProps.values()) {
                                record.set(p.value(), er.get(p.value()));
                            }
                        }
                    });

        }
    }

    /**
     * fetch any existing records for that are in the given RecordSet
     *
     * @param recordSet
     * @param projectId
     * @param expeditionCode
     * @param parentKey
     * @return
     */
    private List<PhotoRecord> getExistingRecords(RecordSet recordSet, int projectId, String expeditionCode, String parentKey) {
        if (projectId == 0 || expeditionCode == null) {
            throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);
        }

        String sql = photosSql.getRecords();
        Map<String, String> tableMap = new HashMap<>();
        tableMap.put("table", PostgresUtils.entityTable(projectId, recordSet.entity().getConceptAlias()));

        List<String[]> idList = new ArrayList<>();

        for (Record record : recordSet.recordsToPersist()) {
            idList.add(new String[]{record.get(parentKey), ((PhotoRecord) record).photoID()});
        }

        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("idList", idList);
        p.addValue("expeditionCode", expeditionCode);

        RowMapper<GenericRecord> rowMapper = new GenericRecordRowMapper();
        return recordRepository.query(
                StrSubstitutor.replace(sql, tableMap),
                p,
                (rs, rowNum) -> new PhotoRecord(rowMapper.mapRow(rs, rowNum).properties()));
    }

    @Override
    public DataConverter newInstance(ProjectConfig projectConfig) {
        return new PhotoConverter(photosSql, recordRepository, projectConfig);
    }
}
