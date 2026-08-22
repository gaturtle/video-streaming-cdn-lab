package com.streaminglab.origin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Scans the Origin's storage directory on demand and resolves video ids to files on disk.
 * A video's id is just its filename — there is no database, the filesystem is the catalog.
 */
@Service
public class VideoCatalogService {

    private final Path storageDir;

    public VideoCatalogService(OriginProperties properties) throws IOException {
        this.storageDir = Paths.get(properties.getPath()).toAbsolutePath().normalize();
        Files.createDirectories(storageDir);
    }

    public List<VideoSummary> list() {
        try (Stream<Path> files = Files.list(storageDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .map(this::toSummary)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public VideoSummary get(String id) {
        return toSummary(resolve(id));
    }

    /** Resolves an id to a file on disk, rejecting anything that would escape the storage dir. */
    public Path resolve(String id) {
        if (!StringUtils.hasText(id) || id.contains("/") || id.contains("\\") || id.contains("..")) {
            throw new NoSuchVideoException(id);
        }
        Path resolved = storageDir.resolve(id).normalize();
        if (!resolved.startsWith(storageDir) || !Files.isRegularFile(resolved)) {
            throw new NoSuchVideoException(id);
        }
        return resolved;
    }

    public Resource resolveResource(String id) {
        return new FileSystemResource(resolve(id));
    }

    /** Stores an upload under its original filename, disambiguating on collision. */
    public VideoSummary store(MultipartFile upload) throws IOException {
        String original = StringUtils.hasText(upload.getOriginalFilename())
                ? StringUtils.getFilename(upload.getOriginalFilename())
                : "upload";
        Path target = storageDir.resolve(original).normalize();
        if (!target.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid filename: " + original);
        }
        target = disambiguate(target);
        upload.transferTo(target);
        return toSummary(target);
    }

    private Path disambiguate(Path target) {
        if (!Files.exists(target)) {
            return target;
        }
        String filename = target.getFileName().toString();
        String base = filename;
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            base = filename.substring(0, dot);
            ext = filename.substring(dot);
        }
        Path parent = target.getParent();
        int suffix = 1;
        Path candidate;
        do {
            candidate = parent.resolve(base + "-" + suffix + ext);
            suffix++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private VideoSummary toSummary(Path path) {
        try {
            String contentType = contentTypeOf(path);
            return new VideoSummary(
                    path.getFileName().toString(),
                    path.getFileName().toString(),
                    Files.size(path),
                    contentType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Files.probeContentType relies on OS mime registrations and can return null for
     * video containers on a bare Linux host, so common extensions are mapped explicitly first.
     */
    private String contentTypeOf(Path path) throws IOException {
        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".mp4") || filename.endsWith(".m4v")) {
            return "video/mp4";
        }
        if (filename.endsWith(".mov")) {
            return "video/quicktime";
        }
        if (filename.endsWith(".webm")) {
            return "video/webm";
        }
        if (filename.endsWith(".mkv")) {
            return "video/x-matroska";
        }
        String probed = Files.probeContentType(path);
        return probed != null ? probed : "application/octet-stream";
    }
}
