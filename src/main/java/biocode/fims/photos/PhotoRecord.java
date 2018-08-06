package biocode.fims.photos;

import biocode.fims.models.records.GenericRecord;

import java.util.Map;

import static biocode.fims.photos.PhotoEntityProps.ORIGINAL_URL;
import static biocode.fims.photos.PhotoEntityProps.PHOTO_ID;

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
}

