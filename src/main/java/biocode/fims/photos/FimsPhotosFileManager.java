package biocode.fims.photos;

import biocode.fims.bcid.ResourceTypes;
import biocode.fims.digester.ChildEntity;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Resource;
import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.reader.JsonTabularDataConverter;
import biocode.fims.reader.ReaderManager;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.renderers.RowMessage;
import biocode.fims.run.ProcessController;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.Hasher;
import biocode.fims.settings.PathManager;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class FimsPhotosFileManager implements AuxilaryFileManager {
    public static final String NAME = "photos";
    public static final String ENTITY_CONCEPT_ALIAS = "fimsPhotos";
    public static final String DATASET_RESOURCE_SUB_TYPE = "fimsPhotos";

    private static final Logger logger = LoggerFactory.getLogger(FimsPhotosFileManager.class);

    private FimsPhotosRepository repository;
    private FimsPhotosQueueRepository queueRepository;
    private BcidService bcidService;
    private ExpeditionService expeditionService;
    private SettingsManager settingsManager;

    private ProcessController processController;
    private ChildEntity entity;
    private Entity parentEntity;
    private String datasetFilename;

    private List<ObjectNode> updatePhotos;
    private List<ObjectNode> createPhotos;
    private Set<String> removePhotos;

    private ArrayNode fimsPhotoMetadata;

    public FimsPhotosFileManager(FimsPhotosRepository repository, FimsPhotosQueueRepository queueRepository,
                                 BcidService bcidService, ExpeditionService expeditionService, SettingsManager settingsManager) {

        this.repository = repository;
        this.queueRepository = queueRepository;
        this.bcidService = bcidService;
        this.expeditionService = expeditionService;
        this.settingsManager = settingsManager;

        updatePhotos = new ArrayList<>();
        createPhotos = new ArrayList<>();
    }


    @Override
    public boolean validate(ArrayNode fimsMetadata) {
        if (processController == null) {
            throw new FimsRuntimeException("Server Error", "processController must not be null", 500);
        }

        if (haveDataset()) {
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

    private boolean haveDataset() {
        return datasetFilename != null;
    }

    private void updateStatus(String message) {
        processController.appendStatus(message);
    }

    @Override
    public void upload(boolean newDataset) {
        if (haveDataset()) {

            try {
                // save the spreadsheet on the server
                File sourceFile = new File(datasetFilename);

                String ext = FileUtils.getExtension(sourceFile.getName(), "xlsx");
                String filename = processController.getProjectId() + "_" + processController.getExpeditionCode() + "_photos." + ext;
                File outputFile = PathManager.createUniqueFile(filename, settingsManager.retrieveValue("serverRoot"));

                Files.copy(sourceFile.toPath(), outputFile.toPath());

                addIdentifiersToPhotosDataset();
                processPhotosDataset();
                updateExistingPhotoResources();
                queueRepository.addToQueue(createPhotos);

                Bcid bcid = new Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE)
                        .ezidRequest(Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequests")))
                        .title("Fims Photo Metadata Dataset: " + processController.getExpeditionCode())
                        .subResourceType(DATASET_RESOURCE_SUB_TYPE)
                        .sourceFile(filename)
                        .finalCopy(processController.getFinalCopy())
                        .build();

                bcidService.create(bcid, processController.getUserId());

                Expedition expedition = expeditionService.getExpedition(
                        processController.getExpeditionCode(),
                        processController.getProjectId()
                );

                bcidService.attachBcidToExpedition(
                        bcid,
                        expedition.getExpeditionId()
                );

                updateStatus("Your Photo metadata has been submitted for uploading. Please allow 24hrs for you photos to appear." +
                        "You will be notified of any errors that occur during processing of your photos.");
            } catch (IOException e) {
                logger.error("failed to save photo metadata dataset input file {}", datasetFilename);
            }
        }

    }

    private void addIdentifiersToPhotosDataset() {
        Bcid rootEntityBcid = expeditionService.getEntityBcid(
                processController.getExpeditionCode(), processController.getProjectId(), entity.getConceptAlias());

        if (rootEntityBcid == null) {
            throw new FimsRuntimeException("Server Error", "rootEntityBcid is null", 500);
        }

        for (JsonNode node: fimsPhotoMetadata) {
            ObjectNode resource = (ObjectNode) node;

            String bcid = rootEntityBcid.getIdentifier() + getSuffix(resource);

            resource.put("bcid", bcid);
        }
    }

    private void updateExistingPhotoResources() {
        repository.updateAndDelete(
                updatePhotos,
                removePhotos
        );
    }

    private void processPhotosDataset() {
        List<Resource> existingPhotoResources = repository.getFimsPhotos(
                processController.getProjectId(),
                processController.getExpeditionCode()
        );

        Map<String, Resource> existingResourceMap = existingPhotoResources.stream()
                .collect(
                        Collectors.toMap(
                                Resource::getBcid,
                                Function.identity()
                        )
                );

        for (JsonNode node : fimsPhotoMetadata) {
            ObjectNode photoNode = (ObjectNode) node;

            String bcid = photoNode.get("bcid").asText();

            // remove it here so we can delete what is left
            if (existingResourceMap.remove(bcid) != null) {
                updatePhotos.add(photoNode);
            } else {
                createPhotos.add(photoNode);
            }
        }

        // anything left should be deleted as it is not in the latest dataset
        removePhotos = existingResourceMap.keySet();
    }

    private String getSuffix(ObjectNode node) {
        String localId = node.get(parentEntity.getUniqueKey()).asText() + node.get(entity.getUniqueKey()).asText();

        Hasher hasher = new Hasher();
        return hasher.hasherDigester(localId);

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
