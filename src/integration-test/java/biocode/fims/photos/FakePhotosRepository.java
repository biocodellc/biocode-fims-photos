package biocode.fims.photos;

import biocode.fims.entities.Resource;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class FakePhotosRepository implements FimsPhotosRepository {
    Map<String, ObjectNode> photos;

    FakePhotosRepository() {
        photos = new HashMap<>();
    }

    @Override
    public List<Resource> getFimsPhotos(int projectId, String expeditionCode) {
        return photos.keySet().stream()
                .map(k -> new Resource(k, photos.get(k)))
                .collect(Collectors.toList());
    }

    @Override
    public void updateAndDelete(Collection<ObjectNode> photoResources, Collection<String> ids) {
        for (String bcid: ids) {
            photos.remove(bcid);
        }

        for (ObjectNode photo: photoResources) {
            String bcid = photo.get("bcid").asText();
            photos.put(bcid, photo);
        }
    }
}
