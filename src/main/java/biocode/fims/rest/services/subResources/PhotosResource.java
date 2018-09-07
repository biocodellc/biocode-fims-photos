package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.PhotosProperties;
import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.PhotoEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.photos.BulkPhotoLoader;
import biocode.fims.photos.BulkPhotoPackage;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.responses.UploadResponse;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.utils.FileUtils;
import biocode.fims.utils.StringGenerator;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipInputStream;

/**
 * @author rjewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class PhotosResource extends FimsController {
    private final static Logger logger = LoggerFactory.getLogger(PhotosResource.class);

    private final ProjectService projectService;
    private final ExpeditionService expeditionService;
    private final ProjectAuthorizer projectAuthorizer;
    private final PhotosProperties photosProps;
    private final RecordRepository recordRepository;
    private final BulkPhotoLoader photoLoader;

    public static enum UploadType {
        NEW, RESUMABLE, RESUME;

        // allows case-insensitive deserialization by jersey
        public static UploadType fromString(String key) {
            return key == null
                    ? null
                    : UploadType.valueOf(key.toUpperCase());
        }

    }

    private static Map<MultiKey, UploadEntry> resumableUploads;

    static {
        resumableUploads = new ConcurrentHashMap<>();
    }

    @Autowired
    public PhotosResource(FimsProperties props, ProjectService projectService, ExpeditionService expeditionService,
                          ProjectAuthorizer projectAuthorizer, PhotosProperties photosProps, RecordRepository recordRepository,
                          BulkPhotoLoader photoLoader) {
        super(props);
        this.projectService = projectService;
        this.expeditionService = expeditionService;
        this.projectAuthorizer = projectAuthorizer;
        this.photosProps = photosProps;
        this.recordRepository = recordRepository;
        this.photoLoader = photoLoader;
    }

    @Authenticated
    @Path("{entity: [a-zA-Z0-9_]+}/upload")
    @PUT
    @Consumes({"application/zip", "application/octet-stream"})
    public UploadResponse bulkUpload(@QueryParam("projectId") Integer projectId,
                                     @QueryParam("expeditionCode") String expeditionCode,
                                     @QueryParam("type") UploadType uploadType,
                                     @PathParam("entity") String conceptAlias,
                                     InputStream is) {
        clearExpiredUploadEntries();

        User user = userContext.getUser();
        Project project = projectService.getProjectWithExpeditions(projectId);

        // valid request logic

        if (project == null) {
            String msg = projectId == null ? "Missing required projectId queryParam" : "Invalid projectId queryParam";
            throw new BadRequestException(msg);
        }

        ProjectConfig config = project.getProjectConfig();
        Entity entity = config.entity(conceptAlias);

        if (entity == null) {
            throw new BadRequestException("Invalid entity path param");
        } else if (!(entity instanceof PhotoEntity)) {
            throw new BadRequestException("Specified entity is not a PhotoEntity");
        }

        Expedition expedition;
        if (expeditionCode == null) {
            Entity parentEntity = config.entity(entity.getParentEntity());
            if (!parentEntity.getUniqueAcrossProject()) {
                throw new BadRequestException("The expeditionCode queryParam is missing. however the uniqueKey for parent:\"" + parentEntity.getConceptAlias() + "\" of entity: \"" + conceptAlias + "\" is not uniqueAcrossProject.");
            }

            if (!projectAuthorizer.userHasAccess(user, project)) {
                throw new ForbiddenRequestException("You do not have permission to upload to this project");
            }

        } else if ((expedition = expeditionService.getExpedition(expeditionCode, projectId)) == null) {
            throw new BadRequestException("Invalid expeditionCode queryParam. That expedition does not exist in this project.");
        } else if (!expedition.getUser().equals(user) && !project.getUser().equals(user)) {
            throw new ForbiddenRequestException("You do not have permission to upload to this project or expedition");
        }

        // process file upload

        MultiKey key = getKey(user, projectId, expeditionCode, conceptAlias);

        UploadEntry uploadEntry = null;
        if (UploadType.RESUME.equals(uploadType)) {
            uploadEntry = resumableUploads.get(key);

            if (uploadEntry == null) {
                throw new BadRequestException("Failed to resume upload. Please try again with a new upload.");
            }
        } else if (UploadType.RESUMABLE.equals(uploadType)) {
            String tempDir = System.getProperty("java.io.tmpdir");
            File targetFile = FileUtils.createUniqueFile(StringGenerator.generateString(20) + ".zip", tempDir);

            uploadEntry = new UploadEntry(projectId, expeditionCode, targetFile);
            uploadEntry.targetFile = targetFile;
            uploadEntry.projectId = projectId;
            uploadEntry.expeditionCode = expeditionCode;

            resumableUploads.put(key, uploadEntry);
        }


        try {
            // resume or resumable upload
            if (uploadEntry != null) {
                try (FileOutputStream fos = new FileOutputStream(uploadEntry.targetFile)) {
                    byte[] buffer = new byte[8192];
                    int size;
                    while ((size = is.read(buffer, 0, buffer.length)) != -1) {
                        fos.write(buffer, 0, size);
                        uploadEntry.size += size;
                        uploadEntry.lastUpdated = ZonedDateTime.now(ZoneOffset.UTC);
                    }
                } catch (EOFException e) {
                    EntityMessages entityMessages = new EntityMessages(conceptAlias);
                    entityMessages.addErrorMessage("Incomplete Upload", new Message("Incomplete file upload"));
                    return new UploadResponse(false, entityMessages);
                }

                // close request InputStream and replace w/ FileInputStream
                is.close();
                is = new FileInputStream(uploadEntry.targetFile);
            }

            // process the bulk upload

            BulkPhotoPackage photoPackage = new BulkPhotoPackage.Builder()
                    .stream(new ZipInputStream(is))
                    .user(user)
                    .project(project)
                    .expeditionCode(expeditionCode)
                    .entity(entity)
                    .photosDir(photosProps.photosDir())
                    .recordRepository(recordRepository)
                    .build();

            // if we are here, we've successfully received the entire file
            UploadResponse uploadResponse = photoLoader.process(photoPackage);

            resumableUploads.remove(key);
            return uploadResponse;
        } catch (IOException e) {
            logger.info("Bulk Upload IOException", e);
            EntityMessages entityMessages = new EntityMessages(conceptAlias);
            entityMessages.addErrorMessage("Incomplete Upload", new Message("Incomplete file upload"));
            return new UploadResponse(false, entityMessages);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Authenticated
    @GET
    @Path("{entity: [a-zA-Z0-9_]+}/upload/progress")
    public UploadEntry status(@QueryParam("projectId") Integer projectId,
                              @QueryParam("expeditionCode") String expeditionCode,
                              @PathParam("entity") String conceptAlias) {
        User user = userContext.getUser();

        MultiKey key = getKey(user, projectId, expeditionCode, conceptAlias);
        UploadEntry uploadEntry = resumableUploads.get(key);

        if (uploadEntry == null) {
            throw new BadRequestException("Failed to find existing upload for the provided params");
        }

        return uploadEntry;
    }

    private void clearExpiredUploadEntries() {
        List<MultiKey> keysToRemove = new ArrayList<>();

        ZonedDateTime expiredTime = ZonedDateTime.now(ZoneOffset.UTC).minusHours(24);

        for (Map.Entry<MultiKey, UploadEntry> e : resumableUploads.entrySet()) {
            if (e.getValue().lastUpdated.isBefore(expiredTime)) {
                keysToRemove.add(e.getKey());
            }
        }

        keysToRemove.forEach(k -> resumableUploads.remove(k));
    }

    private MultiKey getKey(User user, int projectId, String expeditionCode, String conceptAlias) {
        return new MultiKey(user.getUserId(), projectId, expeditionCode, conceptAlias);
    }

    @JsonIgnoreProperties({"projectId", "expeditionCode", "lastUpdated", "targetFile"})
    private class UploadEntry {
        @JsonProperty
        int size;
        int projectId;
        String expeditionCode;
        ZonedDateTime lastUpdated;
        File targetFile;

        UploadEntry(int projectId, String expeditionCode, File targetFile) {
            this.projectId = projectId;
            this.expeditionCode = expeditionCode;
            this.targetFile = targetFile;
        }
    }
}
