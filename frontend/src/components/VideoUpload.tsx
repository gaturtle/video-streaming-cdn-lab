import { useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { uploadVideo } from '@/lib/api'

export function VideoUpload({ onUploaded }: { onUploaded: () => void }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    setError(null)
    try {
      await uploadVideo(file)
      onUploaded()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setUploading(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <div className="flex items-center gap-3">
      <input
        ref={inputRef}
        type="file"
        accept="video/*"
        className="hidden"
        onChange={handleChange}
      />
      <Button disabled={uploading} onClick={() => inputRef.current?.click()}>
        {uploading ? 'Uploading…' : 'Upload video'}
      </Button>
      {error && <span className="text-sm text-destructive">{error}</span>}
    </div>
  )
}
