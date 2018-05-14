package biocode.fims.photos.processing;

import biocode.fims.api.services.AbstractRequest;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.photos.ImageScaler;
import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.application.config.PhotosProperties;
import biocode.fims.settings.PathManager;

import javax.imageio.ImageIO;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author rjewing
 */
public class BasicPhotoProcessor implements PhotoProcessor {
    private final Client client;
    private final PhotosProperties props;

    public BasicPhotoProcessor(Client client, PhotosProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public void process(UnprocessedPhotoRecord record) {
        try {
            // TODO does this work with http & ftp urls?
            // TODO allow specifying root location on PhotoEntity?
            BufferedImage orig = new FileRequest(client, record.originalUrl())
                    .execute();

            ImageScaler scaler = new ImageScaler(orig);

            String name = record.projectId() + "_" + record.parentEntity().getConceptAlias() + "_" + record.entity().getConceptAlias() + "_" + record.projectId();

            String img_128 = this.resize(scaler, name, 128);
            String img_512 = this.resize(scaler, name, 512);
            String img_1024 = this.resize(scaler, name, 1024);

            record.set(PhotoEntityProps.IMG_128.value(), img_128);
            record.set(PhotoEntityProps.IMG_512.value(), img_512);
            record.set(PhotoEntityProps.IMG_1024.value(), img_1024);

        } catch (WebApplicationException e) {
            record.set(PhotoEntityProps.PROCESSING_ERROR.value(), "[\"Failed to fetch originalUrl for processing.\"]");
            throw e;
        } catch (IOException e) {
            record.set(PhotoEntityProps.PROCESSING_ERROR.value(), "[\"Failed to process photo found at originalUrl.\"]");
            throw new FimsRuntimeException(500, e);
        } finally {
            record.set(PhotoEntityProps.PROCESSED.value(), "true");
        }
    }

    private String resize(ImageScaler scaler, String fileNamePrefix, int size) throws IOException {
        File imgFile = PathManager.createUniqueFile(fileNamePrefix + "_" + size, props.photosDir());
        BufferedImage img = scaler.scale(size);
        ImageIO.write(img, "jpg", imgFile);
        return imgFile.getCanonicalPath();
    }

    private static final class FileRequest extends AbstractRequest<BufferedImage> {

        public FileRequest(Client client, String url) {
            super("GET", BufferedImage.class, client, "", url);
            setAccepts("image/*");
        }

    }
}
