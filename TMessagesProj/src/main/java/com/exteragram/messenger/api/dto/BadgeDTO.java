package com.exteragram.messenger.api.dto;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class BadgeDTO {

    private final long documentId;
    private final String badgeId;
    private final String title;
    private String text;

    public BadgeDTO(long documentId, String text) {
        this.documentId = documentId;
        this.badgeId = null;
        this.title = text;
        this.text = text;
    }

    public BadgeDTO(String badgeId, String title, String text) {
        this.documentId = 0;
        this.badgeId = badgeId;
        this.title = title;
        this.text = text;
    }

    public long getDocumentId() {
        return documentId;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public String getTitle() {
        return title != null ? title : text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long component1() {
        return documentId;
    }

    public String component2() {
        return text;
    }

    public BadgeDTO copy(long documentId, String text) {
        return new BadgeDTO(documentId, text);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BadgeDTO)) {
            return false;
        }
        BadgeDTO badge = (BadgeDTO) other;
        return documentId == badge.documentId && Objects.equals(badgeId, badge.badgeId) && Objects.equals(text, badge.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, badgeId, text);
    }

    @NonNull
    @Override
    public String toString() {
        return "BadgeDTO(badgeId=" + badgeId + ", title=" + title + ", text=" + text + ")";
    }
}
