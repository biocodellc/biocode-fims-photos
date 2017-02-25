package biocode.fims.photos;

import biocode.fims.bcid.ResourceTypes;
import biocode.fims.digester.ChildEntity;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.renderers.SheetMessages;
import biocode.fims.run.ProcessController;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.Hasher;
import biocode.fims.settings.SettingsManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import validation.SheetMessagesUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author rjewing
 */
public class FimsPhotoFileManagerUploadTest {
    private static String EXPEDITION_CODE = "integrationTest";
    private static int PROJECT_ID = 0;
    private static String ROOT_BCID = "ark:/99999/s2";

    private Mapping mapping;
    private FimsPhotosFileManager fm;
    private ProcessController pc;
    private ClassLoader classLoader;
    private FakePhotosRepository photoRepository;
    private FakeQueueRepository queueRepository;

    @Mock
    BcidService bcidService;
    @Mock
    ExpeditionService expeditionService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        classLoader = getClass().getClassLoader();
    }

    @Test
    public void upload_with_no_dataset_should_validate_and_do_nothing_on_upload() {
        FimsPhotosFileManager f = setupNoDatasetTest();

        assertTrue(f.validate(null));
        f.upload(true); // this should do nothing
    }

    @Test
    public void new_photo_dataset_adds_all_resources_to_photos_queue() throws URISyntaxException {
        when(expeditionService.getEntityBcid(EXPEDITION_CODE, PROJECT_ID, "fimsPhotos")).thenReturn(getPhotosEntityBcid());
        Expedition expedition = mock(Expedition.class);
        when(expedition.getExpeditionId()).thenReturn(0);
        when(expeditionService.getExpedition(EXPEDITION_CODE, PROJECT_ID)).thenReturn(expedition);

        File datasetFile = new File(classLoader.getResource("validPhotoDataset.csv").getFile());
        init(datasetFile);

        ArrayNode fimsDataset = getFimsMetadata();

        fm.validate(fimsDataset);

        SheetMessages expected = new SheetMessages("Photos");
        JSONObject worksheetMessages = getValidationMessages();

        Assert.assertEquals(SheetMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);

        fm.upload(false);

        // verify that the bcid was created
        Mockito.verify(bcidService).create(any(Bcid.class), anyInt());
        // verify that the bcid was attached to the expedition
        Mockito.verify(bcidService).attachBcidToExpedition(any(Bcid.class), anyInt());

        assertEquals("photoRepository should be empty before any photos have been processed",0, photoRepository.getFimsPhotos(PROJECT_ID, EXPEDITION_CODE).size());
        assertEquals("all photo resources should be added to the queue", 5, queueRepository.queue.size());

        // test that the suffix is generated properly. This should be a hash of the parentEntity uniqueKey field
        // and the childEntity uniqueKey field
        for (ObjectNode photo: queueRepository.queue) {
            String bcid = ROOT_BCID + getSuffix(photo);
            assertEquals("photo bcid should have correct suffix", bcid, photo.get("bcid").asText());
        }
    }

    private String getSuffix(ObjectNode photo) {
        Hasher hasher = new Hasher();
        String localId = photo.get("eventId").asText() + photo.get("photoId").asText();
        return hasher.hasherDigester(localId);
    }

    @Test
    public void photo_dataset_creates_removes_updates_as_expected() throws URISyntaxException {
        when(expeditionService.getEntityBcid(EXPEDITION_CODE, PROJECT_ID, "fimsPhotos")).thenReturn(getPhotosEntityBcid());
        Expedition expedition = mock(Expedition.class);
        when(expedition.getExpeditionId()).thenReturn(0);
        when(expeditionService.getExpedition(EXPEDITION_CODE, PROJECT_ID)).thenReturn(expedition);

        File datasetFile = new File(classLoader.getResource("validPhotoDataset.csv").getFile());
        init(datasetFile);

        photoRepository.photos = populatePhotoRepository();

        ArrayNode fimsDataset = getFimsMetadata();

        fm.validate(fimsDataset);

        SheetMessages expected = new SheetMessages("Photos");
        JSONObject worksheetMessages = getValidationMessages();

        Assert.assertEquals(SheetMessagesUtils.sheetMessagesToJSONObject(expected), worksheetMessages);

        fm.upload(false);


        assertEquals("photoRepository not as expected", expectedPhotoRepository(), photoRepository.photos);
        assertEquals("2 photo resources should be added to the queue", 2, queueRepository.queue.size());

    }

    private Map<String, ObjectNode> populatePhotoRepository() {
        Map<String, ObjectNode> repo = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode photo1 = objectMapper.createObjectNode();
        photo1.put("eventId", "1");
        photo1.put("photoId", "1");
        photo1.put("originalPhotoUrl", "some value");
        photo1.put("photoNotes", "");
        photo1.put("bcid", ROOT_BCID + getSuffix(photo1));
        repo.put(photo1.get("bcid").asText(), photo1);


        ObjectNode photo2 = objectMapper.createObjectNode();
        photo2.put("eventId", "2");
        photo2.put("photoId", "1");
        photo2.put("originalPhotoUrl", "some value");
        photo2.put("photoNotes", "");
        photo2.put("bcid", ROOT_BCID + getSuffix(photo2));
        repo.put(photo2.get("bcid").asText(), photo1);


        ObjectNode photo3 = objectMapper.createObjectNode();
        photo3.put("eventId", "3");
        photo3.put("photoId", "1");
        photo3.put("originalPhotoUrl", "http://ftp.myserver.com/photos/expedition/3.jpg");
        photo3.put("photoNotes", "notes");
        photo3.put("bcid", ROOT_BCID + getSuffix(photo3));
        repo.put(photo3.get("bcid").asText(), photo3);


        ObjectNode photo4 = objectMapper.createObjectNode();
        photo4.put("eventId", "1");
        photo4.put("photoId", "2");
        photo4.put("originalPhotoUrl", "some value");
        photo4.put("photoNotes", "this should be removed");
        photo4.put("bcid", ROOT_BCID + getSuffix(photo4));
        repo.put(photo4.get("bcid").asText(), photo4);

        return repo;
    }

    private Map<String, ObjectNode> expectedPhotoRepository() {
        Map<String, ObjectNode> repo = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode photo1 = objectMapper.createObjectNode();
        photo1.put("eventId", "1");
        photo1.put("photoId", "1");
        photo1.put("originalPhotoUrl", "http://ftp.myserver.com/photos/expedition/1.jpg");
        photo1.put("photoNotes", "this photo was taken at");
        photo1.put("bcid", ROOT_BCID + getSuffix(photo1));
        repo.put(photo1.get("bcid").asText(), photo1);


        ObjectNode photo2 = objectMapper.createObjectNode();
        photo2.put("eventId", "2");
        photo2.put("photoId", "1");
        photo2.put("originalPhotoUrl", "http://ftp.myserver.com/photos/expedition/2.jpg");
        photo2.put("photoNotes", "this photo was taken at");
        photo2.put("bcid", ROOT_BCID + getSuffix(photo2));
        repo.put(photo2.get("bcid").asText(), photo2);


        ObjectNode photo3 = objectMapper.createObjectNode();
        photo3.put("eventId", "3");
        photo3.put("photoId", "1");
        photo3.put("originalPhotoUrl", "http://ftp.myserver.com/photos/expedition/3.jpg");
        photo3.put("photoNotes", "notes");
        photo3.put("bcid", ROOT_BCID + getSuffix(photo3));
        repo.put(photo3.get("bcid").asText(), photo3);

        return repo;
    }

    private Bcid getPhotosEntityBcid() throws URISyntaxException {
        Bcid bcid = new Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE)
                .title(FimsPhotosFileManager.ENTITY_CONCEPT_ALIAS)
                .subResourceType(FimsPhotosFileManager.DATASET_RESOURCE_SUB_TYPE)
                .build();

        bcid.setIdentifier(new URI(ROOT_BCID));
        return bcid;
    }

    private ArrayNode getFimsMetadata() {
        ArrayNode fimsDataset = new ObjectMapper().createArrayNode();
        ObjectNode resource1 = fimsDataset.addObject();
        resource1.put("eventId", "1");
        resource1.put("principalInvestigator", "jack smith");
        ObjectNode resource2 = fimsDataset.addObject();
        resource2.put("eventId", "2");
        resource2.put("principalInvestigator", "jack smith");
        ObjectNode resource3 = fimsDataset.addObject();
        resource3.put("eventId", "3");
        resource3.put("principalInvestigator", "jack smith");
        return fimsDataset;
    }


    private JSONObject getValidationMessages() {
        JSONObject worksheets = (JSONObject) pc.getMessages().get("worksheets");
        return (JSONObject) worksheets.get(mapping.getChildEntities().getFirst().getWorksheet());
    }

    private void init(File datasetFile) {
        File configFile = new File(classLoader.getResource("test.xml").getFile());

        mapping = new Mapping();
        mapping.addMappingRules(configFile);
        Validation validation = new Validation();
        validation.addValidationRules(configFile, mapping);

        pc = new ProcessController(PROJECT_ID, EXPEDITION_CODE);
        pc.setMapping(mapping);
        pc.setValidation(validation);

        String tmpDir = System.getProperty("java.io.tmpdir");
        pc.setOutputFolder(tmpDir);

        SettingsManager sm = mock(SettingsManager.class);
        when(sm.retrieveValue("ezidRequests")).thenReturn("false");
        when(sm.retrieveValue("serverRoot")).thenReturn(tmpDir);

        photoRepository = new FakePhotosRepository();
        queueRepository = new FakeQueueRepository();

        fm = new FimsPhotosFileManager(photoRepository, queueRepository, bcidService, expeditionService, sm);
        try {
            fm.setFilename(datasetFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        fm.setProcessController(pc);
    }


    private FimsPhotosFileManager setupNoDatasetTest() {
        FimsPhotosFileManager f = new FimsPhotosFileManager(null, null, null, null, null);
        String conceptAlias = "Event";

        ChildEntity childEntity = new ChildEntity();
        childEntity.setParentEntityConceptAlias(conceptAlias);
        childEntity.setConceptAlias("fimsPhotos");

        Entity entity = new Entity();
        entity.setConceptAlias(conceptAlias);

        Mapping mapping = new Mapping();
        mapping.addChildEntity(childEntity);
        mapping.addEntity(entity);

        ProcessController p = new ProcessController(0, null);
        p.setMapping(mapping);

        f.setProcessController(p);
        return f;
    }
}
