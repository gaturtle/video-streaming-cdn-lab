package com.streaminglab.origin;

/** One video the Origin knows about: enough for a catalog listing and to build a stream URL. */
public record VideoSummary(String id, String filename, long sizeBytes, String contentType) {
}
