package io.improt.vai.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SubWorkspace {
    private String name;
    private List<String> filePaths;

    // For JSON deserialization
    public SubWorkspace() {
        this.filePaths = new ArrayList<>();
    }

    public SubWorkspace(String name) {
        this.name = name;
        this.filePaths = new ArrayList<>();
    }

    public SubWorkspace(String name, List<String> filePaths) {
        this.name = name;
        this.filePaths = new ArrayList<>(filePaths);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFilePaths() {
        // Return a copy to prevent external modification
        return new ArrayList<>(filePaths);
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = new ArrayList<>(filePaths);
    }

    public void addFilePath(String path) {
        if (!this.filePaths.contains(path)) {
            this.filePaths.add(path);
        }
    }

    public void removeFilePath(String path) {
        this.filePaths.remove(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubWorkspace that = (SubWorkspace) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name; // For JList display
    }
}