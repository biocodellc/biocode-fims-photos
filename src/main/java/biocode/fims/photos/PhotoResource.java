package biocode.fims.photos;

/**
 * @author rjewing
 */
public class PhotoResource {
    private String originalUrl;
    private String bcid;

    public PhotoResource(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getBcid() {
        return bcid;
    }

    public void setBcid(String bcid) {
        this.bcid = bcid;
    }
}
