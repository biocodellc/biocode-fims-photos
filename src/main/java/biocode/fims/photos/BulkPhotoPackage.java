package biocode.fims.photos;

import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.utils.FileUtils;
import biocode.fims.utils.StringGenerator;
import com.opencsv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author rjewing
 */
public class BulkPhotoPackage {
    public final static String BULK_PHOTO_DIR = "bulk";
    public final static String METADATA_FILE_NAME = "metadata.csv";

    private final static Logger logger = LoggerFactory.getLogger(Builder.class);
    // file pattern is parent_identifier+photoID.ext
    private static final Pattern FILE_PATTERN = Pattern.compile("^([a-zA-Z0-9+=:._()~*]+)\\+([a-zA-Z0-9+=:._()~*]+)\\.(.*)?$", Pattern.CASE_INSENSITIVE);

    private final ZipInputStream stream;
    private final User user;
    private final Project project;
    private final String expeditionCode;
    private final RecordRepository recordRepository;
    private final Entity entity;
    private final Entity parentEntity;
    private final ArrayList<String> invalidFiles;
    private final String photosDir;

    private Map<String, String> parentExpeditionCodes;

    private BulkPhotoPackage(Builder builder) {
        this.photosDir = builder.photosDir;
        this.stream = builder.stream;
        this.user = builder.user;
        this.project = builder.project;
        this.expeditionCode = builder.expeditionCode;
        this.entity = builder.entity;
        this.parentEntity = project.getProjectConfig().entity(entity.getParentEntity());
        this.recordRepository = builder.recordRepository;
        this.invalidFiles = new ArrayList<>();
    }

    public File metadataFile() {
        Map<String, List<File>> files = extractFiles();

        if (files.isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        File metadataFile = files.containsKey(METADATA_FILE_NAME)
                ? files.get(METADATA_FILE_NAME).get(0)
                : null;

        if (metadataFile == null) {
            metadataFile = generateMetadataFile(files);
//        } else if (expeditionCode == null && parentEntity.getUniqueAcrossProject()) {
        } else {
            metadataFile = updateMetadataFile(metadataFile);
        }

        return metadataFile;
    }

    public Project project() {
        return project;
    }

    public String expeditionCode() {
        return expeditionCode;
    }

    public ArrayList<String> invalidFiles() {
        return invalidFiles;
    }

    public User user() {
        return user;
    }

    public Entity entity() {
        return entity;
    }

    /**
     * add expeditionCodes to the metadata file if they aren't present
     *
     * @param metadataFile
     * @return
     */
    private File updateMetadataFile(File metadataFile) {
        try {
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<String[]> metadata = new CSVReaderBuilder(new FileReader(metadataFile))
                    .withCSVParser(parser)
                    .build()
                    .readAll();

            List<String> metadataColumns = Arrays.asList(metadata.remove(0));

            // TODO need to update metadata.csv column fileName adding the originalUrl column using the file map
//            if (expeditionCode == null && parentEntity.getUniqueAcrossProject())
            int expeditionCol = metadataColumns.indexOf(Record.EXPEDITION_CODE);

            // if we have an expeditionCode column, we're good to go
            if (expeditionCol > -1) {
                return metadataFile;
            }

            int parentIdentifierCol = metadataColumns.indexOf(parentEntity.getUniqueKey());

            // missing parent identifier column, nothing we can do
            if (parentIdentifierCol == -1) {
                return metadataFile;
            }

            List<String[]> data = new ArrayList<>();

            expeditionCol = metadataColumns.size();
            metadataColumns.add(Record.EXPEDITION_CODE);

            data.add((String[]) metadataColumns.toArray());

            for (String[] row : metadata) {
                row = Arrays.copyOf(row, metadataColumns.size());
                row[expeditionCol] = getExpeditionCode(row[parentIdentifierCol]);
                data.add(row);
            }

            // delete old metadata file
            metadataFile.delete();

            return writeMetadataFile(data);
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.READ_ERROR, 400, METADATA_FILE_NAME);
        }
    }

    private File generateMetadataFile(Map<String, List<File>> files) {
        List<String[]> data = new ArrayList<>();

        data.add(new String[]{
                parentEntity.getUniqueKey(),
                PhotoEntityProps.PHOTO_ID.toString(),
                PhotoEntityProps.ORIGINAL_URL.toString(),
                Record.EXPEDITION_CODE
        });

        for (Map.Entry<String, List<File>> entry : files.entrySet()) {
            Matcher matcher = FILE_PATTERN.matcher(entry.getKey());

            if (matcher.matches()) {
                String parentIdentifier = matcher.group(1);
                String photoId = matcher.group(2);

                for (File f : entry.getValue()) {
                    data.add(new String[]{
                            parentIdentifier,
                            photoId,
                            f.getAbsolutePath(),
                            getExpeditionCode(parentIdentifier)
                    });
                }

            } else {
                invalidFiles.add(entry.getKey());

                for (File img: entry.getValue()) {
                    try {
                        img.delete();
                    } catch (Exception exp) {
                        logger.debug("Failed to delete bulk loaded img file", exp);
                    }
                }
            }
        }

        return writeMetadataFile(data);
    }

    private File writeMetadataFile(List<String[]> data) {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        File file = FileUtils.createUniqueFile(METADATA_FILE_NAME, System.getProperty("java.io.tmpdir"));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            ICSVWriter csvwriter = new CSVWriterBuilder(writer)
                    .withParser(parser)
                    .build();

            csvwriter.writeAll(data);

        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;
    }

    private String getExpeditionCode(String parentIdentifier) {
        if (parentExpeditionCodes == null) {
            fetchParentRecords();
        }
        return parentExpeditionCodes.getOrDefault(parentIdentifier, "");
    }

    private void fetchParentRecords() {
        parentExpeditionCodes = new HashMap<>();

        recordRepository.getRecords(project, parentEntity.getConceptAlias(), GenericRecord.class)
                .forEach(r -> parentExpeditionCodes.put(r.get(parentEntity.getUniqueKeyURI()), r.expeditionCode()));
    }

    private Map<String, List<File>> extractFiles() {
        Map<String, List<File>> files = new HashMap<>();

        Path rootDir = Paths.get(photosDir, BULK_PHOTO_DIR);
        // make rootDir if necessary
        rootDir.toFile().mkdir();

        try {
            ZipEntry ze = stream.getNextEntry();

            // if root ZipEntry is a directory, extract files from there
            String zipRootDir = "";
            if (ze.isDirectory()) {
                zipRootDir = ze.getName();
                ze = stream.getNextEntry();
            }

            List<String> fileSuffixes = Arrays.asList("jpg", "jpeg", "png");

            byte[] buffer = new byte[1024];
            while (ze != null) {
                String fileName = ze.getName().replace(zipRootDir, "");
                String ext = FileUtils.getExtension(fileName, "");

                // ignore nested directories & unsupported file extensions
                if (ze.isDirectory() || !fileSuffixes.contains(ext.toLowerCase())) {
                    logger.info("ignoring dir/unsupported file: " + ze.getName());
                    invalidFiles.add(ze.getName());

                    if (ze.isDirectory()) {
                        // skip everything in that directory
                        String root = ze.getName();

                        do {
                            ze = stream.getNextEntry();
                        }
                        while (ze.getName().startsWith(root));

                    } else {
                        ze = stream.getNextEntry();
                    }
                    continue;
                }

                File file = FileUtils.createUniqueFile(StringGenerator.generateString(20) + "." + ext, rootDir.toString());

                logger.debug("unzipping file: " + fileName + " to: " + file.getAbsolutePath());

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    files.computeIfAbsent(fileName, k -> new ArrayList<>()).add(file);
                } catch (Exception e) {
                    logger.debug("Failed to extract file", e);
                    invalidFiles.add(ze.getName());
                }

                ze = stream.getNextEntry();
            }
        } catch (IOException e) {
            throw new BadRequestException("invalid/corrupt zip file", e);

        }

        return files;
    }

    public static class Builder {
        // required
        private String photosDir;
        private ZipInputStream stream;
        private Project project;
        private User user;
        private Entity entity;
        private RecordRepository recordRepository;

        // optional
        private String expeditionCode;

        public Builder photosDir(String photosDir) {
            this.photosDir = photosDir;
            return this;
        }

        public Builder stream(ZipInputStream stream) {
            this.stream = stream;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        public Builder expeditionCode(String expeditionCode) {
            this.expeditionCode = expeditionCode;
            return this;
        }

        public Builder entity(Entity entity) {
            this.entity = entity;
            return this;
        }

        public Builder recordRepository(RecordRepository recordRepository) {
            this.recordRepository = recordRepository;
            return this;
        }

        private boolean isValid() {
            return photosDir != null &&
                    stream != null &&
                    recordRepository != null &&
                    entity != null &&
                    user != null &&
                    project != null;
        }

        public BulkPhotoPackage build() {
            if (!isValid()) {
                throw new ServerErrorException("Server Error", "photosDir, stream, recordRepository, entity, user, and project are all required");
            }
            return new BulkPhotoPackage(this);
        }
    }
}
