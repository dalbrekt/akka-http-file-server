package akkahttp.javadsl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * Created by <a href="https://github.com/dalbrekt">Tony Dalbrekt</a>.
 */
public class FileInfo {
    private String fileName;
    private String targetFile;
    private String size;


    public FileInfo(
            @JsonProperty("fileName") String fileName,
            @JsonProperty("targetFile") String targetFile,
            @JsonProperty("size") String size) {

        this.fileName = fileName;
        this.targetFile = targetFile;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public String getSize() {
        return size;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fileName", fileName)
                .add("targetFile", targetFile)
                .add("size", size)
                .toString();
    }
}
