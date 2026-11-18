package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.entity.Topic;
import java.util.List;

public interface TopicService {

    List<Topic> findAll();

    List<Topic> bySubject(Long id);

    Topic save(Topic topic);

}
