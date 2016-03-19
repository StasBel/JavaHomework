package ru.spbau.mit;

/**
 * Created by belaevstanislav on 14.03.16.
 */

public class File {
    private final String name;
    private final boolean isDir;

    public File(String name, boolean isDir) {
        this.name = name;
        this.isDir = isDir;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof File) {
            File that = (File) obj;
            if ((name.equals(that.name)) && (isDir == that.isDir)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
