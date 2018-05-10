package biocode.fims.photos;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.RecordMetadata;

import java.util.Map;

import static biocode.fims.photos.PhotoProps.ORIGINAL_URL;
import static biocode.fims.photos.PhotoProps.PHOTO_ID;

/**
 * @author rjewing
 */
public class PhotoRecord extends GenericRecord {

//    public PhotoRecord(String parentUniqueKeyUri, String identifier, RecordMetadata recordMetadata) {
//        super();
//        properties.put(SEQUENCE_URI, sequence);
//        properties.put(parentUniqueKeyUri, identifier);

//        for (Map.Entry e : recordMetadata.metadata().entrySet()) {
//            properties.put((String) e.getKey(), (String) e.getValue());
//        }
//    }

    public String originalUrl() {
        return properties.get(ORIGINAL_URL.value());
    }

    public String photoID() {
        return properties.get(PHOTO_ID.value());
    }
}

