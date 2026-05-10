package net.tmn.storage_manager.service;

public record UploadedImage(String filename, String contentType, byte[] bytes) {

    public boolean isEmpty() {
        return bytes == null || bytes.length == 0;
    }

    public long size() {
        return bytes == null ? 0 : bytes.length;
    }
}
