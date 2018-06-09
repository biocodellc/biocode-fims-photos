package biocode.fims.photos.reader;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.application.config.PhotosSql;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.projectConfig.models.PhotoEntity;
import biocode.fims.reader.DataReader;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.utils.FileUtils;

import java.io.File;
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
    private PhotoConverter converter;

    public PhotoReader(PhotosSql photosSql, RecordRepository recordRepository, List<DataReader> tabularReaders) {
        this.photosSql = photosSql;
        this.recordRepository = recordRepository;
        this.tabularReaders = tabularReaders;
        this.converter = new PhotoConverter(photosSql, recordRepository);
    }

    public PhotoReader(PhotosSql photosSql, RecordRepository recordRepository, List<DataReader> tabularReaders, File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        this.photosSql = photosSql;
        this.recordRepository = recordRepository;
        this.tabularReaders = tabularReaders;
        this.file = file;
        this.config = projectConfig;
        this.recordMetadata = recordMetadata;
        this.converter = new PhotoConverter(photosSql, recordRepository, projectConfig);
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
                recordSets.add(converter.convertRecordSet(recordSet, projectId, expeditionCode));
            }
        }

        return recordSets;
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
