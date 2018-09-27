package biocode.fims.photos;

import biocode.fims.records.GenericRecord;

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

    public String originalUrl() {
        return properties.get(ORIGINAL_URL.value());
    }

    public String photoID() {
        return properties.get(PHOTO_ID.value());
    }

    public boolean bulkLoad() {
        return bulkLoadFile() != null;
    }

    public String bulkLoadFile() {
        return properties.get(BULK_LOAD_FILE.value());
    }
}

