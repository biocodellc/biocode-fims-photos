package biocode.fims.photos;

import biocode.fims.digester.EntityProps;

/**
 * @author rjewing
 */
public enum PhotoProps implements EntityProps {
    PHOTO_ID("photoID"),
    ORIGINAL_URL("originalUrl"),
    PROCESSED("processed");

    private final String val;

    PhotoProps(String val) {
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
