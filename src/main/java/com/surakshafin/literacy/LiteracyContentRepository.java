package com.surakshafin.literacy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiteracyContentRepository extends JpaRepository<LiteracyContent, Long> {
    List<LiteracyContent> findByLanguage(String language);
    List<LiteracyContent> findByTopicAndLanguage(String topic, String language);
}
