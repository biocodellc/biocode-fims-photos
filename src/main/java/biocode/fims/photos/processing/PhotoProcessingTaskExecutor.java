package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(PhotoProcessingTaskExecutor.class);
    private static final Set<PhotoRecord> PHOTOS_IN_PROCESSING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ExecutorService executorService;

    public PhotoProcessingTaskExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    // TODO maybe need to set a timeout? https://geowarin.github.io/completable-futures-with-spring-async.html
    public void addTask(PhotoProcessingTask task) {
        if (PHOTOS_IN_PROCESSING.add(task.record())) {
            logger.info("submitting task for: " + task.record());

            CompletableFuture.runAsync(task, executorService)
                    .whenComplete((v, err) -> {
                        PHOTOS_IN_PROCESSING.remove(task.record());
                        if (err != null) {
                            logger.error(err.getMessage(), err);
                        }
                    });

        } else {
            logger.info("ignoring duplicate task: " + task.record());
        }
    }
}
