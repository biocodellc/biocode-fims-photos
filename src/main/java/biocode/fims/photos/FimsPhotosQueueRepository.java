package biocode.fims.photos;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;

/**
 * @author rjewing
 */
public interface FimsPhotosQueueRepository {
    void addToQueue(Collection<ObjectNode> values);
}
