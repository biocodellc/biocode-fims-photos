package biocode.fims.config.models;

import biocode.fims.config.Config;
import biocode.fims.config.models.PropEntity;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.validation.rules.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;

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
        getAttribute(BULK_LOAD_FILE.value()).setInternal(true);
        recordType = PhotoRecord.class;

        // note: default rules are set in the PhotoValidator
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void addDefaultRules(Config config) {
        super.addDefaultRules(config);
        RequiredValueRule requiredValueRule = getRule(RequiredValueRule.class, RuleLevel.ERROR);

        if (requiredValueRule == null) {
            requiredValueRule = new RequiredValueRule(new LinkedHashSet<>(), RuleLevel.ERROR);
            addRule(requiredValueRule);
        }

        requiredValueRule.addColumn(PHOTO_ID.value());

        LinkedHashSet<String> columns = new LinkedHashSet<>(Arrays.asList(ORIGINAL_URL.value(), BULK_LOAD_FILE.value()));
        RequiredValueInGroupRule requiredValueInGroupRule = new RequiredValueInGroupRule(columns, RuleLevel.ERROR);
        addRule(requiredValueInGroupRule);
    }

    @Override
    public Entity clone() {
        PhotoEntity entity = new PhotoEntity(getConceptAlias());

        getRules().forEach(r -> {
            // TODO create a Rule method clone()
            // hacky way to make a copy of the rule
            Rule newR = JacksonUtil.fromString(
                    JacksonUtil.toString(r),
                    r.getClass()
            );
            entity.addRule(newR);
        });
        getAttributes().forEach(a -> entity.addAttribute(a.clone()));

        entity.setParentEntity(getParentEntity());
        entity.recordType = recordType;

        entity.setWorksheet(getWorksheet());
        entity.setUniqueKey(getUniqueKey());
        entity.setUniqueAcrossProject(getUniqueAcrossProject());
        entity.setHashed(isHashed());

        return entity;
    }

    /**
     * class used to verify PhotoEntity data integrity after deserialization. This is necessary
     * so we don't overwrite the default values during deserialization.
     */
    static class PhotoEntitySanitizer extends PropEntitySanitizer<PhotoEntity> {
    }
}

