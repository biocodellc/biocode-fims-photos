package biocode.fims.photos;

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
}
