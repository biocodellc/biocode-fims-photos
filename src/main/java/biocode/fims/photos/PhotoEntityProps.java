package biocode.fims.photos;

import biocode.fims.config.models.EntityProps;

/**
 * @author rjewing
 */
public enum PhotoEntityProps implements EntityProps {
    PHOTO_ID("photoID"),
    ORIGINAL_URL("originalUrl"),
    BULK_LOAD("bulkLoad"),
    PROCESSED("processed"),
    PROCESSING_ERROR("imageProcessingErrors"),
    IMG_128("img128"),
    IMG_512("img512"),
    IMG_1024("img1024");

    private final String val;

    PhotoEntityProps(String val) {
        this.val = val;
    }

    @Override
    public String value() {
        return val;
    }

    @Override
    public String toString() {
        return val;
    }
}
