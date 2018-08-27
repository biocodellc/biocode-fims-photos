package biocode.fims.config.project.models;

import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.validation.rules.RequiredValueRule;
import biocode.fims.validation.rules.RuleLevel;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static biocode.fims.photos.PhotoEntityProps.*;


/**
 * @author rjewing
 */
@JsonDeserialize(converter = PhotoEntity.PhotoEntitySanitizer.class)
public class PhotoEntity extends PropEntity<PhotoEntityProps> {
    private static final String CONCEPT_URI = "http://rs.tdwg.org/dwc/terms/associatedMedia";
    public static final String TYPE = "Photo";

    private PhotoEntity() { // needed for EntityTypeIdResolver
        super(PhotoEntityProps.class);
    }

    public PhotoEntity(String conceptAlias) {
        super(PhotoEntityProps.class, conceptAlias, CONCEPT_URI);
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

    @Override
    public void addDefaultRules(ProjectConfig config) {
        super.addDefaultRules(config);

        RequiredValueRule requiredValueRule = getRule(RequiredValueRule.class, RuleLevel.ERROR);
        requiredValueRule.addColumn(PHOTO_ID.value());
        requiredValueRule.addColumn(ORIGINAL_URL.value());
    }

    /**
     * class used to verify PhotoEntity data integrity after deserialization. This is necessary
     * so we don't overwrite the default values during deserialization.
     */
    static class PhotoEntitySanitizer extends PropEntitySanitizer<PhotoEntity> {
    }
}

