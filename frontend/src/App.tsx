import { useCallback, useEffect, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { listVideos, type VideoSummary } from '@/lib/api'
import { VideoPlayer } from '@/components/VideoPlayer'
import { VideoUpload } from '@/components/VideoUpload'
import { cn } from '@/lib/utils'

function App() {
  const [videos, setVideos] = useState<VideoSummary[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(() => {
    listVideos()
      .then((vs) => {
        setVideos(vs)
        setError(null)
        setSelectedId((current) => current ?? vs[0]?.id ?? null)
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load videos'))
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  const selected = videos.find((v) => v.id === selectedId) ?? null

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6 p-8">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Origin video streaming</h1>
        <VideoUpload onUploaded={refresh} />
      </header>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Card>
        <CardHeader>
          <CardTitle>Player</CardTitle>
        </CardHeader>
        <CardContent>
          {selected ? (
            <VideoPlayer video={selected} />
          ) : (
            <p className="text-sm text-muted-foreground">
              No videos yet — upload one to get started.
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Videos on the Origin</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          {videos.map((v) => (
            <Button
              key={v.id}
              variant={v.id === selectedId ? 'secondary' : 'ghost'}
              className={cn('justify-start', v.id === selectedId && 'font-medium')}
              onClick={() => setSelectedId(v.id)}
            >
              {v.filename}
            </Button>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}

export default App
