package ru.spbau.mit;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public class File {
    private static final int HASHCODE_BASE = 31;
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
            return name.equals(that.name) && isDir == that.isDir;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = name.hashCode() * HASHCODE_BASE;
        if (isDir) {
            result += 1;
        } else {
            result += 0;
        }
        return result;
    }
}
