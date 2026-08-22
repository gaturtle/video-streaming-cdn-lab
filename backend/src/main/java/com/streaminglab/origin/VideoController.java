package com.streaminglab.origin;

import java.io.IOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The Origin's HTTP surface: list what's on disk, upload a new master file, and stream
 * one by byte range so a browser <video> tag can seek without downloading the whole file.
 */
@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = {"http://localhost:5173"})
public class VideoController {

    /** Caps how much of an open-ended range ("bytes=1000-") is sent in one response. */
    private static final long MAX_CHUNK_SIZE = 1024 * 1024;

    private final VideoCatalogService catalog;

    public VideoController(VideoCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<VideoSummary> list() {
        return catalog.list();
    }

    @GetMapping("/{id}")
    public VideoSummary get(@PathVariable String id) {
        return catalog.get(id);
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<ResourceRegion> stream(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        VideoSummary summary = catalog.get(id);
        Resource video = catalog.resolveResource(id);
        long contentLength = summary.sizeBytes();
        MediaType mediaType = MediaType.parseMediaType(summary.contentType());
        List<HttpRange> ranges = headers.getRange();

        HttpStatus status;
        ResourceRegion region;
        if (ranges.isEmpty()) {
            status = HttpStatus.OK;
            region = new ResourceRegion(video, 0, contentLength);
        } else {
            status = HttpStatus.PARTIAL_CONTENT;
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(MAX_CHUNK_SIZE, end - start + 1);
            region = new ResourceRegion(video, start, rangeLength);
        }

        return ResponseEntity.status(status)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(mediaType)
                .body(region);
    }

    @PostMapping
    public ResponseEntity<VideoSummary> upload(@RequestParam("file") MultipartFile file) throws IOException {
        VideoSummary stored = catalog.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }
}
