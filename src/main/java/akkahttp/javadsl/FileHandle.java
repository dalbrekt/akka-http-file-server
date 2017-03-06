package akkahttp.javadsl;

import com.google.common.base.MoreObjects;

/**
 * Created by <a href="https://github.com/dalbrekt">Tony Dalbrekt</a>.
 */
public class FileHandle {
    private final FileInfo info;

    public FileHandle(FileInfo info) {
        this.info = info;
    }

    public FileInfo getInfo() {
        return info;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("info", info)
                .toString();
    }
}
