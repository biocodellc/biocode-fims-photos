package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoEntityProps;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskTest {

    @Test
    public void initialize_and_run() {
        UnprocessedPhotoRecord record = getPhotoRecord();
        FakePhotoProcessor photoProcessor = new FakePhotoProcessor();
        PhotoProcessingTask processingTask = new PhotoProcessingTask(photoProcessor, record);

        assertEquals(record, processingTask.record());
        assertFalse(photoProcessor.processed);

        processingTask.run();

        assertTrue(photoProcessor.processed);
    }

    private UnprocessedPhotoRecord getPhotoRecord() {
        UnprocessedPhotoRecord photo = new UnprocessedPhotoRecord(null, null, 0, 0);
        photo.set(PhotoEntityProps.PROCESSED.value(), "false");
        photo.set(PhotoEntityProps.PHOTO_ID.value(), "Photo1");
        photo.set(PhotoEntityProps.ORIGINAL_URL.value(), "ftp:///photo/location");
        return photo;
    }

}