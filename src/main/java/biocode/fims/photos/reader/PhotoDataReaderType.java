package biocode.fims.photos.reader;

import biocode.fims.reader.DataReader;

/**
 * @author rjewing
 */
public class PhotoDataReaderType {
    public static final String READER_TYPE_STRING = "PHOTO";
    public static final DataReader.DataReaderType READER_TYPE = new DataReader.DataReaderType(READER_TYPE_STRING);

    private PhotoDataReaderType() {}
}
