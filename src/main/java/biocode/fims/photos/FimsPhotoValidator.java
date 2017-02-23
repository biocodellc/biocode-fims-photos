package biocode.fims.photos;

import biocode.fims.renderers.SheetMessages;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;

/**
 * @author rjewing
 */
public class FimsPhotoValidator {
    private final ArrayNode dataset;
    private SheetMessages sheetMessages;

    public FimsPhotoValidator(ArrayNode dataset) {
        this.dataset = dataset;
        this.sheetMessages = new SheetMessages();
    }

    public boolean validate(List<String> parentResourceIds) {

        for (JsonNode node: dataset) {
            parentResourceIds.contains(node.at("resourceId").asText());
        }
        return true;
    }

    public SheetMessages getValidationMessages() {
        return sheetMessages;
    }
}
