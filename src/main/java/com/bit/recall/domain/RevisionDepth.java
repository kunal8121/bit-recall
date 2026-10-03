package com.bit.recall.domain;

import lombok.Getter;

@Getter
public enum RevisionDepth {
    QUICK("Quick revision, focusing on key points and summaries."),
    BALANCED("Balanced revision, covering essential details without overwhelming depth."),
    COMPREHENSIVE("Comprehensive revision, providing in-depth coverage of all relevant information.");

    private final String description;

    RevisionDepth(String s) {
        this.description = s;
    }
}
