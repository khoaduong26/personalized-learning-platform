package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.entity.Lesson;

import java.util.List;

public interface LessonService {

    List<Lesson> findAll();

    List<Lesson> byTopic(Long id);

    Lesson save(Lesson lesson);
}