package biocode.fims.photos.processing;

import biocode.fims.digester.Entity;
import biocode.fims.models.Project;
import biocode.fims.models.records.GenericRecordRowMapper;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.photos.application.config.PhotosSql;
import biocode.fims.photos.digester.PhotoEntity;
import biocode.fims.query.PostgresUtils;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ProjectService;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.jdbc.core.RowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskScheduler {

    private final ProjectService projectService;
    private final RecordRepository recordRepository;
    private final PhotosSql photosSql;
    private final PhotoProcessingTaskExecutor processingTaskExecutor;

    public PhotoProcessingTaskScheduler(ProjectService projectService, RecordRepository recordRepository, PhotosSql photosSql, PhotoProcessingTaskExecutor processingTaskExecutor) {
        this.projectService = projectService;
        this.recordRepository = recordRepository;
        this.photosSql = photosSql;
        this.processingTaskExecutor = processingTaskExecutor;
    }

    //TODO @Scheduled
    public void scheduleTasks() {
        for (Project p : projectService.getProjects()) {
            for (Entity e : p.getProjectConfig().entities()) {

                if (!(e instanceof PhotoEntity)) return;

                String sql = photosSql.unprocessedPhotos();
                Map<String, String> tableMap = new HashMap<>();
                tableMap.put("table", PostgresUtils.entityTable(p.getProjectId(), e.getConceptAlias()));

                RowMapper rowMapper = new GenericRecordRowMapper();
                List<PhotoRecord> records = recordRepository.query(
                        StrSubstitutor.replace(sql, tableMap),
                        new HashMap<>(),
                        (rs, rowNum) -> (PhotoRecord) rowMapper.mapRow(rs, rowNum));

                for (PhotoRecord record : records) {
                    PhotoProcessingTask task = new PhotoProcessingTask(new BasicPhotoProcessor(), record);
                    processingTaskExecutor.addTask(task);
                }
            }
        }
    }
}
