package com.surakshafin.literacy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "literacy_content")
@Getter
@Setter
@NoArgsConstructor
public class LiteracyContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String format = "ARTICLE";
}
