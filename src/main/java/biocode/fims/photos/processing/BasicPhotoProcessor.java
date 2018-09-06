package biocode.fims.photos.processing;

import biocode.fims.api.services.AbstractRequest;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.photos.ImageScaler;
import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.application.config.PhotosProperties;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.utils.FileUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ResponseProcessingException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author rjewing
 */
public class BasicPhotoProcessor implements PhotoProcessor {
    private final static Logger logger = LoggerFactory.getLogger(BasicPhotoProcessor.class);

    private final Client client;
    private final PhotosProperties props;

    public BasicPhotoProcessor(Client client, PhotosProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public void process(UnprocessedPhotoRecord record) {
        try {
            BufferedImage orig;

            if (record.bulkLoad()) {
                orig = ImageIO.read(new File(record.originalUrl()));
            } else {
                orig = new FileRequest(client, record.originalUrl())
                        .execute();
            }

            ImageScaler scaler = new ImageScaler(orig);

            removeExistingImages(record);

            Path dir = Paths.get(props.photosDir(), String.valueOf(record.projectId()), record.entity().getConceptAlias());
            // make dirs if necessary
            dir.toFile().mkdirs();

            // strip any query params from the url
            String originalUrl = record.originalUrl().replaceFirst("\\?.*", "");
            String formatName = FileUtils.getExtension(originalUrl, "jpg");

            String img_128 = this.resize(scaler, dir.toString(), record.expeditionCode() + "_" + record.photoID(), formatName, 128);
            String img_512 = this.resize(scaler, dir.toString(), record.expeditionCode() + "_" + record.photoID(), formatName, 512);
            String img_1024 = this.resize(scaler, dir.toString(), record.expeditionCode() + "_" + record.photoID(), formatName, 1024);

            if (record.bulkLoad()) {
                try {
                    File img = new File(record.originalUrl());
                    img.delete();
                } catch (Exception e) {
                    logger.debug("Failed to delete bulk loaded img file", e);
                }
                record.set(PhotoEntityProps.ORIGINAL_URL.value(), null);
            }

            record.set(PhotoEntityProps.IMG_128.value(), img_128);
            record.set(PhotoEntityProps.IMG_512.value(), img_512);
            record.set(PhotoEntityProps.IMG_1024.value(), img_1024);
            record.set(PhotoEntityProps.PROCESSING_ERROR.value(), null);

        } catch (ResponseProcessingException | WebApplicationException e) {
            record.set(
                    PhotoEntityProps.PROCESSING_ERROR.value(),
                    "[\"Failed to fetch originalUrl for processing. Is the file accessible at \"" + record.originalUrl() + "\" and a valid image type?\"]"
            );
            throw e;
        } catch (IOException e) {
            String msg;
            if (record.bulkLoad()) {
                msg = "[\"Failed to process photo from bulk load. Image may be corrupt\"]";

                try {
                    File img = new File(record.originalUrl());
                    img.delete();
                } catch (Exception exp) {
                    logger.debug("Failed to delete bulk loaded img file", exp);
                }
            } else {
                msg = "[\"Failed to process photo found at originalUrl.\"]";
            }

            record.set(PhotoEntityProps.PROCESSING_ERROR.value(), msg);
            throw new FimsRuntimeException(500, e);
        } finally {
            record.set(PhotoEntityProps.PROCESSED.value(), "true");
        }
    }

    /**
     * attempt to remove any existing images if possible.
     *
     * @param record
     */
    private void removeExistingImages(PhotoRecord record) {
        String img_128 = record.get(PhotoEntityProps.IMG_128.value());
        if (!img_128.trim().equals("")) removeImg(img_128);

        String img_512 = record.get(PhotoEntityProps.IMG_512.value());
        if (!img_512.trim().equals("")) removeImg(img_512);

        String img_1024 = record.get(PhotoEntityProps.IMG_1024.value());
        if (!img_1024.trim().equals("")) removeImg(img_1024);
    }

    private void removeImg(String url) {
        String[] split = url.split("/");
        String name = split[split.length - 1];

        File imgFile = new File(props.photosDir(), name);

        if (imgFile.exists()) {
            try {
                imgFile.delete();
            } catch (Exception e) {
                logger.warn("Failed to delete img file: ", imgFile.getAbsolutePath());
            }
        }
    }

    private String resize(ImageScaler scaler, String dir, String fileNamePrefix, String formatName, int size) throws IOException {
        File imgFile = FileUtils.createUniqueFile(fileNamePrefix + "_" + size + "." + formatName, dir);
        BufferedImage img = scaler.scale(size);
        ImageIO.write(img, formatName, imgFile);

        return imgFile.getCanonicalPath().replace(Paths.get(props.photosDir()).toFile().getCanonicalPath() + "/", props.photosRoot());
    }

    private static final class FileRequest extends AbstractRequest<BufferedImage> {

        public FileRequest(Client client, String url) {
            super("GET", BufferedImage.class, client, "", url);
            setAccepts("image/*");
        }

    }
}
