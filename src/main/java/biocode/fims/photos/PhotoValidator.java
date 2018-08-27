package biocode.fims.photos;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.ValidatorInstantiator;

/**
 * @author rjewing
 */
public class PhotoValidator extends RecordValidator {

    public PhotoValidator(ProjectConfig config) {
        super(config);
    }

    public static class PhotoValidatorInstantiator implements ValidatorInstantiator {
        @Override
        public RecordValidator newInstance(ProjectConfig config) {
            return new PhotoValidator(config);
        }
    }
}
