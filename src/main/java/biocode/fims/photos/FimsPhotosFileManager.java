package biocode.fims.photos;

import biocode.fims.digester.ChildEntity;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.reader.JsonTabularDataConverter;
import biocode.fims.reader.ReaderManager;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.renderers.RowMessage;
import biocode.fims.run.ProcessController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class FimsPhotosFileManager implements AuxilaryFileManager {
    public static final String NAME = "photos";
    public static final String ENTITY_CONCEPT_ALIAS = "fimsPhotos";

    private ProcessController processController;
    private ChildEntity entity;
    private Entity parentEntity;
    private String datasetFilename;
    private FimsPhotosPersistenceManager persistenceManager;
    private ArrayNode fimsPhotoMetadata;


    @Override
    public boolean validate(ArrayNode fimsMetadata) {
        if (processController == null) {
            throw new FimsRuntimeException("Server Error", "processController must not be null", 500);
        }

        if (validateDataset()) {
            updateStatus("\nRunning fims photos dataset validation.");

            String uniqueKey = parentEntity.getUniqueKey();
            List<String> parentIds = getDatasetIdentifiers(fimsMetadata, uniqueKey);

            return validatePhotoDataset() && photoMetadataHasMatchingResource(parentIds);
        }

        return true;
    }

    private boolean photoMetadataHasMatchingResource(List<String> parentIds) {

        if (parentIds.isEmpty()) {
            processController.addMessage(
                    entity.getWorksheet(),
                    new RowMessage("No parent resources found.", "Spreadsheet check", RowMessage.ERROR)
            );
            return false;
        }

        ArrayList<String> invalidIds = new ArrayList<>();

        for (String identifier : getPhotoResourceIds()) {
            if (!parentIds.contains(identifier)) {
                invalidIds.add(identifier);
            }
        }

        if (!invalidIds.isEmpty()) {
            int level;
            // this is an error if no ids exist in the dataset
            if (invalidIds.size() == fimsPhotoMetadata.size()) {
                level = RowMessage.ERROR;
            } else {
                level = RowMessage.WARNING;
                processController.setHasWarnings(true);
            }
            processController.addMessage(
                    entity.getWorksheet(),
                    new RowMessage(StringUtils.join(invalidIds, ", "),
                            "The following " + parentEntity.getUniqueKey() + "'s do not exist.", level)
            );
            if (level == RowMessage.ERROR) {
                return false;
            }
        }

        return true;
    }

    private List<String> getPhotoResourceIds() {
        return getDatasetIdentifiers(fimsPhotoMetadata, parentEntity.getUniqueKey());
    }

    private List<String> getDatasetIdentifiers(ArrayNode dataset, String uniqueKey) {
        List<String> resourceIds = new ArrayList<>();

        for (JsonNode node : dataset) {
            ObjectNode resource = (ObjectNode) node;
            if (resource.has(uniqueKey)) {
                resourceIds.add(resource.get(uniqueKey).asText());
            }
        }

        return resourceIds;
    }

    private boolean validatePhotoDataset() {
        Validation validation = processController.getValidation();
        String outputPrefix = processController.getExpeditionCode() + "photos_output";

        String sheetName = entity.getWorksheet();

        // Create the tabularDataReader for reading the input file
        ReaderManager rm = new ReaderManager();
        rm.loadReaders();
        TabularDataReader tdr = rm.openFile(datasetFilename, sheetName, processController.getOutputFolder());

        if (tdr == null) {
            processController.appendStatus("<br>Unable to open the file you attempted to upload.<br>");
            return false;
        }

        try {
            JsonTabularDataConverter tdc = new JsonTabularDataConverter(tdr);
            fimsPhotoMetadata = tdc.convert(entity.getAttributes(), sheetName);

            // Run the validation
            validation.run(tdr, outputPrefix, processController.getOutputFolder(), processController.getMapping(), fimsPhotoMetadata, sheetName);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() != null) {
                processController.addMessage(sheetName, new RowMessage(e.getUsrMessage(), "Initial Spreadsheet check", RowMessage.ERROR));
                return false;
            } else {
                throw e;
            }
        } finally {
            tdr.closeFile();
        }

        // get the Messages from each worksheet and add them to the processController
        processController.addMessages(validation.getMessages());

        if (validation.hasErrors()) {
            return false;
        } else if (validation.hasWarnings()) {
            processController.setHasWarnings(true);
        }

        return true;
    }

    private boolean validateDataset() {
        return datasetFilename != null;
    }

    private void updateStatus(String message) {
        processController.appendStatus(message);
    }

    @Override
    public void upload(boolean newDataset) {

    }

    @Override
    public void index(ArrayNode dataset) {

    }

    public void setFilename(String filename) {
        if (this.datasetFilename != null) {
            throw new FimsRuntimeException("Server Error", "You can only upload 1 fimsPhoto metadata dataset at a time", 500);
        }
        this.datasetFilename = filename;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void setProcessController(ProcessController processController) {
        this.processController = processController;

        Mapping mapping = processController.getMapping();
        entity = mapping.findChildEntity(ENTITY_CONCEPT_ALIAS);
        parentEntity = mapping.findEntity(entity.getParentEntityConceptAlias());
    }

    @Override
    public void close() {
        if (datasetFilename != null) {
            new File(datasetFilename).delete();
        }
    }
}
