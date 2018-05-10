package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoProps;
import biocode.fims.photos.PhotoRecord;
import org.junit.Test;

import java.util.concurrent.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskExecutorTest {

    @Test
    public void duplicate_queuedPhotos_not_registered_with_executor_service() {
        ExecutorService executorService = mock(ExecutorService.class);
        PhotoProcessingTaskExecutor executor = new PhotoProcessingTaskExecutor(executorService);

        PhotoProcessingTask processingTask = getPhotoProcessingTask();

        executor.addTask(processingTask);
        executor.addTask(processingTask);

        verify(executorService, times(1)).submit(processingTask);
    }

    private PhotoProcessingTask getPhotoProcessingTask() {
        PhotoRecord photo = new PhotoRecord();
        photo.set(PhotoProps.PROCESSED.value(), "false");
        photo.set(PhotoProps.PHOTO_ID.value(), "Photo1");
        return new PhotoProcessingTask(null, photo);
    }

}