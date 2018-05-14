package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoRecord;

/**
 * @author rjewing
 */
public class FakePhotoProcessor implements PhotoProcessor {
    boolean processed = false;

    @Override
    public void process(PhotoRecord record) {
        processed = true;
    }
}
