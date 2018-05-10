package biocode.fims.photos;

import biocode.fims.photos.queue.FimsPhotosQueueRepository;
import biocode.fims.photos.queue.QueuedPhoto;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * @author rjewing
 */
public class FakeQueueRepository implements FimsPhotosQueueRepository {
    List<ObjectNode> queue;

    public FakeQueueRepository() {
        this.queue = new ArrayList<>();
    }

    @Override
    public void addToQueue(Collection<ObjectNode> values) {
        queue.addAll(values);
    }

    @Override
    public Set<QueuedPhoto> getQueuedPhotos() {
        return null;
    }
}
