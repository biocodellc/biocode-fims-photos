package biocode.fims.photos.digester;

import biocode.fims.digester.PropEntity;
import biocode.fims.photos.PhotoProps;
import biocode.fims.photos.PhotoRecord;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static biocode.fims.photos.PhotoProps.PHOTO_ID;


/**
 * @author rjewing
 */
@JsonDeserialize(converter = PhotoEntity.PhotoEntitySanitizer.class)
public class PhotoEntity extends PropEntity<PhotoProps> {
    private static final String CONCEPT_URI = "PhotoMetadata";
    public static final String TYPE = "Photo";


    private PhotoEntity() { // needed for EntityTypeIdResolver
        super();
    }

    public PhotoEntity(String conceptAlias) {
        super(PhotoProps.class, conceptAlias, CONCEPT_URI);
    }

    @Override
    protected void init() {
        super.init();

        // This is actually a composite unique_key. The actual key is
        // parentEntityUniqueKey_photoEntityUniqueKey
        // TODO look into possibility of using photo hashing, but worried about false positives
        setUniqueKey(PHOTO_ID.value());
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

