package biocode.fims.photos.processing;

import biocode.fims.config.models.Entity;
import biocode.fims.photos.PhotoRecord;

import java.util.Map;

/**
 * @author rjewing
 */
public class UnprocessedPhotoRecord extends PhotoRecord {

    private final int networkId;
    private final int expeditionId;
    private final Entity entity;
    private final Entity parentEntity;

    public UnprocessedPhotoRecord(Entity parentEntity, Entity entity, int networkId, int expeditionId) {
        super();
        this.parentEntity = parentEntity;
        this.entity = entity;
        this.networkId = networkId;
        this.expeditionId = expeditionId;
    }

    public UnprocessedPhotoRecord(Map<String, String> properties, Entity parentEntity, Entity entity, int networkId, int expeditionId) {
        super(properties);
        this.parentEntity = parentEntity;
        this.entity = entity;
        this.networkId = networkId;
        this.expeditionId = expeditionId;
    }

    public int networkId() {
        return networkId;
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
