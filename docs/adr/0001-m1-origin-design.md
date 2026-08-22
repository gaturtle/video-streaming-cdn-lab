# 0001: M1 Origin design — module layout, API shape, catalog

## Status

Accepted.

## Context

Milestone 1 needed a Spring Boot Origin serving a large MP4 from local disk with
correct HTTP Range support, a simple upload endpoint, and a React/shadcn player
page that plays and seeks it. See `CONTEXT.md` for the Origin / Range Request
vocabulary this design maps onto.

## Decisions

- **Two top-level modules**: `backend/` (Spring Boot 4.1.1, Java 21, Maven) and
  `frontend/` (Vite + React + TS + Tailwind v4 + shadcn/ui), run as separate dev
  processes. Vite's dev server proxies `/api` to `localhost:8080` so the frontend
  never needs CORS in dev; `@CrossOrigin` on the controller covers direct access.
- **The filesystem *is* the catalog.** `VideoCatalogService` scans
  `origin.storage.path` (default `../media`, override via `ORIGIN_STORAGE_PATH`)
  on every list/resolve call — no database. A video's id is its filename;
  `resolve()` rejects any id containing `/`, `\`, or `..` and verifies the
  resolved path stays under the storage root, since the id comes straight from
  the URL path.
- **`origin.storage.path` is bound as `String`, not `java.nio.file.Path`.**
  Spring Boot 4's `Path` property binder treats a leading `..` as an escaping
  classpath resource path and fails startup (`/../media` normalizes to `null`).
  Converting via `Paths.get(...)` after binding avoids that binder entirely.
- **Range support via `ResourceRegion`** (`VideoController.stream`), following
  Spring's own reference pattern: parse `HttpRange` from the request, build a
  `ResourceRegion` over a `FileSystemResource`, return 206 with `Content-Range`
  when a `Range` header is present and 200 without one, and always add
  `Accept-Ranges: bytes`. An open-ended range (`bytes=1000-`) is capped to 1MiB
  per response rather than streaming to EOF in one shot.
- **Content-Type is guessed from the file extension first** (`mp4`→`video/mp4`,
  etc.), falling back to `Files.probeContentType`. A bare Linux host (the VPS)
  often has no mimetypes DB registered, so relying on the JDK probe alone
  returns `application/octet-stream` for videos, which some browsers refuse to
  play regardless of Range support.
- **Upload is one `multipart/form-data` POST** (`POST /api/videos`, field
  `file`) that stores under the original filename, disambiguating on
  collision (`name-1.ext`, `name-2.ext`, ...). No chunking/resumability — that
  concern belongs to the sibling `huge-file-transfer-lab` project per the map's
  Notes.
- **The sample `.mov` needed a lossless remux to `.mp4`** before Chrome would
  play it — Chrome checks a `<source>`'s declared MIME type before ever
  requesting it, and never requests `video/quicktime`. Confirmed by the browser
  making zero network requests against a `.mov` source vs. playing the `.mp4`
  immediately. `ffmpeg -c copy` (stream copy, no re-encode) produced
  `media/big_buck_bunny_720p.mp4` from `media/big_buck_bunny_720p_h264.mov`.

## Verified

- `curl` against `/api/videos/{id}/stream`: no `Range` header → 200 with
  `Content-Range: bytes 0-N/N+1`; `Range: bytes=0-1023` → 206,
  `Content-Range: bytes 0-1023/...`, `Content-Length: 1024`; open-ended and
  suffix ranges (`bytes=1000-`, `bytes=-500`) resolve correctly and the
  open-ended case is capped to 1MiB.
- Upload endpoint verified via `curl -F`, including collision disambiguation.
- Real-browser playback and seeking verified via Chrome automation: play
  advances `currentTime`/`buffered`; seeking to 300s produces a new,
  non-contiguous buffered range (`[300, 320]`) distinct from the initial
  `[0, 28]` — i.e., the player is issuing fresh byte-range requests rather
  than downloading the file linearly.

## Not covered here

- Deploying M1 to the VPS (copying `backend/`, `frontend` build output, and
  the remuxed `media/*.mp4` there, running the Origin as a service). This
  session had no SSH access; see the map's Not yet specified.
- HLS/DASH, transcoding, and CDN — M2 onward.
