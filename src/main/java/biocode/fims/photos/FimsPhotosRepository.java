package biocode.fims.photos;

import biocode.fims.entities.Resource;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.List;

/**
 * @author rjewing
 */
public interface FimsPhotosRepository {

    List<Resource> getFimsPhotos(int projectId, String expeditionCode);

    void updateAndDelete(Collection<ObjectNode> photoResources, Collection<String> ids);
}
