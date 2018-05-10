package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoProps;
import biocode.fims.photos.PhotoRecord;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskTest {

    @Test
    public void initialize_and_run() {
        PhotoRecord record = getPhotoRecord();
        FakePhotoProcessor photoProcessor = new FakePhotoProcessor();
        PhotoProcessingTask processingTask = new PhotoProcessingTask(photoProcessor, record);

        assertEquals(record, processingTask.record());
        assertFalse(photoProcessor.processed);

        processingTask.run();

        assertTrue(photoProcessor.processed);
    }

    private PhotoRecord getPhotoRecord() {
        PhotoRecord photo = new PhotoRecord();
        photo.set(PhotoProps.PROCESSED.value(), "false");
        photo.set(PhotoProps.PHOTO_ID.value(), "Photo1");
        photo.set(PhotoProps.ORIGINAL_URL.value(), "ftp:///photo/location");
        return photo;
    }

}