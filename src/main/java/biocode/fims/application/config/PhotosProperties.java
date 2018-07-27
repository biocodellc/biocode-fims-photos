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
        String dir = env.getRequiredProperty("photosDir");
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        return dir;
    }

    public String photosRoot() {
        String base = env.getRequiredProperty("appRoot");
        String path = env.getRequiredProperty("photosRoot");

        if (!path.endsWith("/")) {
            path += "/";
        }

        if (base.endsWith("/") && path.startsWith("/")) {
            return base + path.replaceFirst("/", "");
        } else if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        } else {
            return base + path;
        }
    }
}
