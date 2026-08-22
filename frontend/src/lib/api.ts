export interface VideoSummary {
  id: string
  filename: string
  sizeBytes: number
  contentType: string
}

export async function listVideos(): Promise<VideoSummary[]> {
  const res = await fetch('/api/videos')
  if (!res.ok) throw new Error(`Failed to list videos: ${res.status}`)
  return res.json()
}

export function streamUrl(id: string): string {
  return `/api/videos/${encodeURIComponent(id)}/stream`
}

export async function uploadVideo(file: File): Promise<VideoSummary> {
  const body = new FormData()
  body.append('file', file)
  const res = await fetch('/api/videos', { method: 'POST', body })
  if (!res.ok) throw new Error(`Upload failed: ${res.status}`)
  return res.json()
}
