package biocode.fims.photos.processing;

import biocode.fims.projectConfig.models.Entity;
import biocode.fims.photos.PhotoRecord;

import java.util.Map;

/**
 * @author rjewing
 */
public class UnprocessedPhotoRecord extends PhotoRecord {

    private final int projectId;
    private final int expeditionId;
    private final Entity entity;
    private final Entity parentEntity;

    public UnprocessedPhotoRecord(Entity parentEntity, Entity entity, int projectId, int expeditionId) {
        super();
        this.parentEntity = parentEntity;
        this.entity = entity;
        this.projectId = projectId;
        this.expeditionId = expeditionId;
    }

    public UnprocessedPhotoRecord(Map<String, String> properties, Entity parentEntity, Entity entity, int projectId, int expeditionId) {
        super(properties);
        this.parentEntity = parentEntity;
        this.entity = entity;
        this.projectId = projectId;
        this.expeditionId = expeditionId;
    }

    public int projectId() {
        return projectId;
    }

    public int expeditionId() {
        return expeditionId;
    }

    public Entity entity() {
        return entity;
    }

    public Entity parentEntity() {
        return parentEntity;
    }
}
