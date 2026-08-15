# Video Streaming & CDN Lab

A learning effort building a Java (Spring Boot) + React/shadcn video streaming pipeline, staged from origin-only playback through adaptive bitrate to a real CDN edge — deployed on the user's own VPS + domain.

## Language

**Origin (Origin Server)**:
The Java backend serving video bytes from the VPS's own disk, with no CDN in front. The baseline every later stage is compared against.
_Avoid_: Source server, backend (backend is fine informally; Origin is canonical when contrasting with CDN-fronted requests)

**Range Request**:
An HTTP request for a byte range of a file (the `Range` header) that the Origin must honor with a `206 Partial Content` response, `Content-Range` header, and `Accept-Ranges: bytes` — the mechanism that lets a browser `<video>` tag seek and play a large file without downloading it whole. Milestone 1's core mechanic.
_Avoid_: Chunk (Chunk is huge-file-transfer-lab's upload-side term; a Range Request is requested on demand by the player, not pre-cut in advance)

**Rendition**:
One quality-level encoding of a video (e.g. 360p/1200kbps, 1080p/5000kbps) produced by transcoding. Adaptive bitrate playback switches between Renditions of the same video as bandwidth changes.
_Avoid_: Version, quality (quality is fine informally; Rendition is canonical for the artifact)

**Segment**:
A short (~2-10s) time-slice of one Rendition, stored as its own file, that an adaptive-bitrate player requests one at a time. Distinct from a Range Request: Segments are pre-cut at transcode time; a Range Request is a byte-offset ask against a single whole file.
_Avoid_: Chunk, part

**Manifest**:
The playlist file describing a video's available Renditions and their Segments — `.m3u8` for HLS, `.mpd` for DASH — that the player downloads first and uses to decide what to request next.
_Avoid_: Playlist (informal synonym; Manifest is canonical), index

**Transcode**:
The server-side ffmpeg step that takes one uploaded master video file and produces the set of Renditions + Segments + Manifest needed for adaptive playback. Milestone 2/3's core mechanic.
_Avoid_: Encode (encode is the underlying codec operation; Transcode is the canonical name for the whole pipeline step)

**Edge (CDN Edge)**:
A Cloudflare point-of-presence sitting in front of the Origin, caching responses so repeat requests for the same bytes don't hit the Origin. Milestone 4's core mechanic.
_Avoid_: CDN (CDN is the service as a whole; Edge is the specific cache node handling a given request)

**Cache Hit / Cache Miss**:
Whether the Edge already had the requested bytes cached (Hit, served without touching Origin) or had to fetch them from Origin first (Miss). The observable signal that the CDN is doing anything at all.
_Avoid_: none

## Decision records

See `docs/adr/` for standing architectural decisions as they're made.
