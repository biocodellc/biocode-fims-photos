package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoRecord;

/**
 * @author rjewing
 */
public class PhotoProcessingTask implements Runnable {

    private final PhotoProcessor photoProcessor;
    private final PhotoRecord record;

    public PhotoProcessingTask(PhotoProcessor photoProcessor, PhotoRecord record) {
        this.photoProcessor = photoProcessor;
        this.record = record;
    }

    @Override
    public void run() {
        photoProcessor.process();
    }

    public PhotoRecord record() {
        return record;
    }
}
