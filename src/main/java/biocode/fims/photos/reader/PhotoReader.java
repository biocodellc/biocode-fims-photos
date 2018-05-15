package biocode.fims.photos.reader;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.GenericRecordRowMapper;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.application.config.PhotosSql;
import biocode.fims.digester.PhotoEntity;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.PostgresUtils;
import biocode.fims.reader.DataReader;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.utils.FileUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.io.File;
import java.sql.Types;
import java.util.*;

/**
 * @author rjewing
 */
public class PhotoReader implements DataReader {
    private final PhotosSql photosSql;
    private final RecordRepository recordRepository;
    private final List<DataReader> tabularReaders;
    protected File file;
    protected ProjectConfig config;
    private RecordMetadata recordMetadata;

    public PhotoReader(PhotosSql photosSql, RecordRepository recordRepository, List<DataReader> tabularReaders) {
        this.photosSql = photosSql;
        this.recordRepository = recordRepository;
        this.tabularReaders = tabularReaders;
    }

    public PhotoReader(PhotosSql photosSql, RecordRepository recordRepository, List<DataReader> tabularReaders, File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        this.photosSql = photosSql;
        this.recordRepository = recordRepository;
        this.tabularReaders = tabularReaders;
        this.file = file;
        this.config = projectConfig;
        this.recordMetadata = recordMetadata;
    }

    @Override
    public List<RecordSet> getRecordSets(int projectId, String expeditionCode) {
        String ext = FileUtils.getExtension(file.getAbsolutePath(), "");
        DataReader reader = getReader(ext);

        // this shouldn't happen as handlesExtension should be called first to say if
        // this reader is appropriate
        if (reader == null) throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);

        reader = reader.newInstance(file, config, recordMetadata);

        List<RecordSet> recordSets = new ArrayList<>();

        for (RecordSet recordSet : reader.getRecordSets(projectId, expeditionCode)) {
            if (recordSet.entity() instanceof PhotoEntity) {
                recordSets.add(recordSet);

                String parent = recordSet.entity().getParentEntity();
                String parentKey = config.entity(parent).getUniqueKeyURI();

                List<PhotoRecord> existingRecords = getExistingRecords(recordSet, projectId, expeditionCode, parentKey);
                updateRecords(recordSet, existingRecords, parentKey);
            }
        }

        return recordSets;
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
                        // if the originalUrl is the same, we don't need to process any longer
                        // TODO possibly need to persist more data?
                        if (er.originalUrl().equals(record.originalUrl())) {
                            record.set(PhotoEntityProps.PROCESSED.value(), "true");
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
            idList.add(new String[]{record.get(parentKey), ((PhotoRecord) record).photoID() });
        }

        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("idList", idList);
        p.addValue("expeditionCode", expeditionCode);

        RowMapper rowMapper = new GenericRecordRowMapper();
        return recordRepository.query(
                StrSubstitutor.replace(sql, tableMap),
                p,
                (rs, rowNum) -> (PhotoRecord) rowMapper.mapRow(rs, rowNum));
    }

    @Override
    public boolean handlesExtension(String ext) {
        return getReader(ext) != null;
    }

    private DataReader getReader(String ext) {
        for (DataReader r : tabularReaders) {
            if (r.handlesExtension(ext)) return r;
        }
        return null;
    }

    @Override
    public DataReader newInstance(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        return new PhotoReader(photosSql, recordRepository, tabularReaders, file, projectConfig, recordMetadata);
    }

    @Override
    public DataReaderType readerType() {
        return PhotoDataReaderType.READER_TYPE;
    }
}
