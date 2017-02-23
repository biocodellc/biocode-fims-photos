package biocode.fims.photos;

import biocode.fims.renderers.SheetMessages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class FimsPhotoValidatorTest {

    @Test
    public void valid_given_no_dataset() {
        FimsPhotoValidator validator = new FimsPhotoValidator(new ArrayNode(null));

        boolean valid = validator.validate(Collections.emptyList());
        SheetMessages validationMessages = validator.getValidationMessages();

        assertTrue(valid);
        assertTrue("valid photo dataset contains no error messages", validationMessages.getErrorMessages().isEmpty());
    }

    @Test
    public void valid_given_each_photo_resource_has_valid_parent_resource() {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode dataset = objectMapper.createArrayNode();

        ObjectNode photoResource = dataset.addObject();
        photoResource.put("resourceId", "1");

        FimsPhotoValidator validator = new FimsPhotoValidator(dataset);

        List<String> parentResourceIds = Arrays.asList("1", "2", "3");
        boolean valid = validator.validate(parentResourceIds);

        SheetMessages validationMessages = validator.getValidationMessages();

        assertTrue(valid);
        assertTrue("valid photo dataset contains no error messages", validationMessages.getErrorMessages().isEmpty());
    }

    @Test
    public void invalid_given_a_photo_resource_missing_parent_resource() {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode dataset = objectMapper.createArrayNode();

        ObjectNode photoResource = dataset.addObject();
        photoResource.put("resourceId", "2");

        FimsPhotoValidator validator = new FimsPhotoValidator(dataset);

        List<String> parentResourceIds = Arrays.asList("1");
        boolean valid = validator.validate(parentResourceIds);

        SheetMessages validationMessages = validator.getValidationMessages();

        assertFalse(valid);
        assertFalse("invalid photo dataset contains error messages", validationMessages.getErrorMessages().isEmpty());
    }

}