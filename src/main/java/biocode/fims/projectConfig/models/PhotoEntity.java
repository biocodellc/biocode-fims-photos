package biocode.fims.projectConfig.models;

import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.photos.PhotoRecord;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static biocode.fims.photos.PhotoEntityProps.*;


/**
 * @author rjewing
 */
@JsonDeserialize(converter = PhotoEntity.PhotoEntitySanitizer.class)
public class PhotoEntity extends PropEntity<PhotoEntityProps> {
    private static final String CONCEPT_URI = "http://rs.tdwg.org/dwc/terms/associatedMedia";
    public static final String TYPE = "Photo";

    private String photosRoot;


    private PhotoEntity() { // needed for EntityTypeIdResolver
        super(PhotoEntityProps.class);
    }

    public PhotoEntity(String conceptAlias) {
        super(PhotoEntityProps.class, conceptAlias, CONCEPT_URI);
    }

    public String photosRoot() {
        return photosRoot;
    }

    public void photosRoot(String photosRoot) {
        this.photosRoot = photosRoot;
    }

    @Override
    protected void init() {
        super.init();

        // TODO look into possibility of using photo hashing, but worried about false positives
        setUniqueKey(PHOTO_ID.value());
        getAttribute(PROCESSED.value()).setInternal(true);
        getAttribute(PROCESSING_ERROR.value()).setInternal(true);
        recordType = PhotoRecord.class;

        // note: default rules are set in the PhotoValidator
    }

    @Override
    public String type() {
        return TYPE;
    }

    /**
     * class used to verify PhotoEntity data integrity after deserialization. This is necessary
     * so we don't overwrite the default values during deserialization.
     */
    static class PhotoEntitySanitizer extends PropEntitySanitizer<PhotoEntity> {}
}

