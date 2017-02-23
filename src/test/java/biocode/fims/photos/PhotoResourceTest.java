package biocode.fims.photos;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author rjewing
 */
public class PhotoResourceTest {

    @Test
    public void createObject() {
        String originalUrl = "http://example.com/myImg.jpg";
        PhotoResource photoResource = new PhotoResource(originalUrl);
        assertEquals(null, photoResource.getBcid());

        String bcid = "ark:/99999/Aex2test";

        photoResource.setBcid(bcid);

        assertEquals(bcid, photoResource.getBcid());
        assertEquals(originalUrl, photoResource.getOriginalUrl());
    }

}