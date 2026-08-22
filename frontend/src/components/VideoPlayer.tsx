import { streamUrl, type VideoSummary } from '@/lib/api'

function formatSize(bytes: number): string {
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(1)} MiB`
}

export function VideoPlayer({ video }: { video: VideoSummary }) {
  return (
    <div className="flex flex-col gap-2">
      {/* key forces a fresh <video> element (and a fresh Range request) per video */}
      <video key={video.id} className="w-full rounded-lg border" controls preload="metadata">
        <source src={streamUrl(video.id)} type={video.contentType} />
      </video>
      <p className="text-sm text-muted-foreground">
        {video.filename} · {formatSize(video.sizeBytes)}
      </p>
    </div>
  )
}
