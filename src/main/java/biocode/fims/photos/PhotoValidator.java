package biocode.fims.photos;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.ValidatorInstantiator;
import biocode.fims.validation.rules.RequiredValueRule;
import biocode.fims.validation.rules.Rule;
import biocode.fims.validation.rules.RuleLevel;

import java.util.Set;

import static biocode.fims.photos.PhotoEntityProps.ORIGINAL_URL;
import static biocode.fims.photos.PhotoEntityProps.PHOTO_ID;

/**
 * @author rjewing
 */
public class PhotoValidator extends RecordValidator {

    public PhotoValidator(ProjectConfig config) {
        super(config);
    }

    @Override
    protected void addDefaultRules(Set<Rule> rules, RecordSet recordSet) {
        super.addDefaultRules(rules, recordSet);

        Entity entity = recordSet.entity();

        RequiredValueRule requiredValueRule = entity.getRule(RequiredValueRule.class, RuleLevel.ERROR);
        requiredValueRule.addColumn(PHOTO_ID.value());
        requiredValueRule.addColumn(ORIGINAL_URL.value());
    }

    public static class PhotoValidatorInstantiator implements ValidatorInstantiator {
        @Override
        public RecordValidator newInstance(ProjectConfig config) {
            return new PhotoValidator(config);
        }
    }
}
