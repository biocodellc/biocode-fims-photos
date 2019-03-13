package biocode.fims.photos;

import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;

import java.util.HashMap;
import java.util.Map;

import static biocode.fims.photos.PhotoEntityProps.*;

/**
 * @author rjewing
 */
public class PhotoRecord extends GenericRecord {

    public PhotoRecord() {
        super();
    }

    public PhotoRecord(Map<String, String> properties) {
        super(properties);
    }

    private PhotoRecord(Map<String, String> properties, String rootIdentifier, int projectId, String expeditionCode, boolean shouldPersist) {
        super(properties, rootIdentifier, projectId, expeditionCode, shouldPersist);
    }

    public String originalUrl() {
        return properties.get(ORIGINAL_URL.uri());
    }

    public String photoID() {
        return properties.get(PHOTO_ID.uri());
    }

    public boolean bulkLoad() {
        return bulkLoadFile() != null;
    }

    public String bulkLoadFile() {
        return properties.get(BULK_LOAD_FILE.uri());
    }

    @Override
    public Record clone() {
        return new PhotoRecord(new HashMap<>(properties), rootIdentifier(), projectId(), expeditionCode(), persist);
    }
}

