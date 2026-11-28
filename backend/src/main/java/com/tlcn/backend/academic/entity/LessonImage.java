package com.tlcn.backend.academic.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@Embeddable
public class LessonImage {
    @Column(length = 500)
    private String url;
    @Column(length = 200)
    private String altText;
    @Column(length = 300)
    private String caption;
    @Column(nullable = false, length = 24)
    private String placement;
    @Column(nullable = false, length = 16)
    private String alignment;
    @Column(nullable = false, length = 16)
    private String displaySize;

    protected LessonImage() {}

    public LessonImage(String url, String altText, String caption, String placement, String alignment, String displaySize) {
        this.url = url;
        this.altText = altText;
        this.caption = caption;
        this.placement = placement;
        this.alignment = alignment;
        this.displaySize = displaySize;
    }

    public String getUrl() { return url; }
    public String getAltText() { return altText; }
    public String getCaption() { return caption; }
    public String getPlacement() { return placement; }
    public String getAlignment() { return alignment; }
    public String getDisplaySize() { return displaySize; }
}
