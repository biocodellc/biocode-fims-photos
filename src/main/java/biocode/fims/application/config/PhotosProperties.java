package biocode.fims.application.config;

import org.springframework.core.env.Environment;

/**
 * @author rjewing
 */
public class PhotosProperties {
    private final Environment env;

    public PhotosProperties(Environment env) {
        this.env = env;
    }

    public String photosDir() {
        return env.getRequiredProperty("photosDir");
    }
}
