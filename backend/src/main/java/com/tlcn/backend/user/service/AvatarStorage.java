package com.tlcn.backend.user.service;

public interface AvatarStorage {
    String upload(Long userId, byte[] image, String contentType);
}
