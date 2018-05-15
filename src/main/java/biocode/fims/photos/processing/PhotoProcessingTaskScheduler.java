package biocode.fims.photos.processing;

import biocode.fims.digester.Entity;
import biocode.fims.models.Project;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.application.config.PhotosProperties;
import biocode.fims.application.config.PhotosSql;
import biocode.fims.digester.PhotoEntity;
import biocode.fims.query.PostgresUtils;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ProjectService;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.scheduling.annotation.Scheduled;

import javax.ws.rs.client.Client;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskScheduler {
    private static final long TEN_MINS = 1000 * 60 * 10;

    private final ProjectService projectService;
    private final RecordRepository recordRepository;
    private final PhotosSql photosSql;
    private final PhotoProcessingTaskExecutor processingTaskExecutor;
    private final Client client;
    private final PhotosProperties props;

    public PhotoProcessingTaskScheduler(ProjectService projectService, RecordRepository recordRepository,
                                        PhotosSql photosSql, PhotoProcessingTaskExecutor processingTaskExecutor,
                                        Client client, PhotosProperties props) {
        this.projectService = projectService;
        this.recordRepository = recordRepository;
        this.photosSql = photosSql;
        this.processingTaskExecutor = processingTaskExecutor;
        this.client = client;
        this.props = props;
    }

    @Scheduled(fixedDelay = TEN_MINS)
    public void scheduleTasks() {
        PhotoProcessor photoProcessor = new BasicPhotoProcessor(client, props);
        for (Project p : projectService.getProjects()) {
            for (Entity e : p.getProjectConfig().entities()) {
                if (!(e instanceof PhotoEntity)) continue;

                Entity parentEntity = p.getProjectConfig().entity(e.getParentEntity());

                String sql = photosSql.unprocessedPhotos();
                Map<String, String> tableMap = new HashMap<>();
                tableMap.put("table", PostgresUtils.entityTable(p.getProjectId(), e.getConceptAlias()));

                List<UnprocessedPhotoRecord> records = recordRepository.query(
                        StrSubstitutor.replace(sql, tableMap),
                        null,
                        (rs, rowNum) -> {
                            String data = rs.getString("data");
                            int expeditionId = rs.getInt("expedition_id");

                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, String> properties = JacksonUtil.fromString(data, HashMap.class);
                                return new UnprocessedPhotoRecord(properties, parentEntity, e, p.getProjectId(), expeditionId);
                            } catch (Exception ex) {
                                throw new SQLException(ex);
                            }
                        });

                for (UnprocessedPhotoRecord record : records) {
                    PhotoProcessingTask task = new PhotoProcessingTask(photoProcessor, record);
                    processingTaskExecutor.addTask(task);
                }
            }
        }
    }
}
