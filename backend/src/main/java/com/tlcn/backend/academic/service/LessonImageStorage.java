package com.tlcn.backend.academic.service;

public interface LessonImageStorage {
    String uploadLessonImage(Long ownerId, byte[] image, String contentType);
}
