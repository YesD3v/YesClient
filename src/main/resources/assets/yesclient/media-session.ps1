$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

try {
    Add-Type -AssemblyName System.Runtime.WindowsRuntime
    Add-Type -AssemblyName System.Drawing

    $script:asTaskMethod = [System.WindowsRuntimeSystemExtensions].GetMethods() |
        Where-Object {
            $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and
            $_.GetGenericArguments().Length -eq 1 -and
            $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
        } |
        Select-Object -First 1

    function Wait-WinRtOperation($operation, [Type]$resultType) {
        $task = $script:asTaskMethod.MakeGenericMethod($resultType).Invoke($null, [object[]]@($operation))
        if (-not $task.Wait(8000)) { throw 'Windows media operation timed out' }
        return $task.Result
    }

    function Test-BraveLogoThumbnail([byte[]]$imageBytes, [string]$sourceAppId) {
        if ($sourceAppId -notmatch '(?i)brave' -or $null -eq $imageBytes -or $imageBytes.Length -eq 0) {
            return $false
        }

        $imageStream = $null
        $thumbnail = $null
        $thumbnailSample = $null
        $thumbnailGraphics = $null
        $icon = $null
        $iconBitmap = $null
        $iconSample = $null
        $iconGraphics = $null
        try {
            $process = Get-Process -Name 'brave' -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($null -eq $process -or [string]::IsNullOrWhiteSpace($process.Path)) { return $false }

            $imageStream = [System.IO.MemoryStream]::new($imageBytes)
            $thumbnail = [System.Drawing.Image]::FromStream($imageStream)
            $icon = [System.Drawing.Icon]::ExtractAssociatedIcon($process.Path)
            if ($null -eq $icon) { return $false }
            $iconBitmap = $icon.ToBitmap()
            $thumbnailSample = [System.Drawing.Bitmap]::new(16, 16)
            $iconSample = [System.Drawing.Bitmap]::new(16, 16)
            $thumbnailGraphics = [System.Drawing.Graphics]::FromImage($thumbnailSample)
            $iconGraphics = [System.Drawing.Graphics]::FromImage($iconSample)
            $thumbnailGraphics.Clear([System.Drawing.Color]::White)
            $iconGraphics.Clear([System.Drawing.Color]::White)
            $thumbnailGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $iconGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $thumbnailGraphics.DrawImage($thumbnail, 0, 0, 16, 16)
            $iconGraphics.DrawImage($iconBitmap, 0, 0, 16, 16)

            $difference = 0L
            for ($y = 0; $y -lt 16; $y++) {
                for ($x = 0; $x -lt 16; $x++) {
                    $left = $thumbnailSample.GetPixel($x, $y)
                    $right = $iconSample.GetPixel($x, $y)
                    $difference += [Math]::Abs([int]$left.R - [int]$right.R)
                    $difference += [Math]::Abs([int]$left.G - [int]$right.G)
                    $difference += [Math]::Abs([int]$left.B - [int]$right.B)
                }
            }
            $similarity = 1.0 - ($difference / (16.0 * 16.0 * 3.0 * 255.0))
            return $similarity -ge 0.84
        } catch {
            return $false
        } finally {
            if ($null -ne $iconGraphics) { $iconGraphics.Dispose() }
            if ($null -ne $thumbnailGraphics) { $thumbnailGraphics.Dispose() }
            if ($null -ne $iconSample) { $iconSample.Dispose() }
            if ($null -ne $thumbnailSample) { $thumbnailSample.Dispose() }
            if ($null -ne $iconBitmap) { $iconBitmap.Dispose() }
            if ($null -ne $icon) { $icon.Dispose() }
            if ($null -ne $thumbnail) { $thumbnail.Dispose() }
            if ($null -ne $imageStream) { $imageStream.Dispose() }
        }
    }

    $managerType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
    $mediaPropertiesType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime]
    $streamType = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage, ContentType=WindowsRuntime]
    $readerType = [Windows.Storage.Streams.DataReader, Windows.Storage, ContentType=WindowsRuntime]
    $manager = Wait-WinRtOperation ($managerType::RequestAsync()) $managerType
    $lastTrackKey = ''
    $lastArtworkKey = ''
    $lastArtworkAttempt = [DateTime]::MinValue
    $artworkAttempts = 0
    $maximumArtworkAttempts = 5

    while ($true) {
        $snapshot = [ordered]@{
            active = $false
            title = ''
            artist = ''
            trackKey = ''
            playing = $false
            position = 0.0
            duration = 0.0
            updatedAt = 0
            cover = ''
        }

        try {
            $session = $manager.GetCurrentSession()
            if ($null -ne $session) {
                $properties = Wait-WinRtOperation ($session.TryGetMediaPropertiesAsync()) $mediaPropertiesType
                $title = [string]$properties.Title
                $artist = [string]$properties.Artist
                $album = [string]$properties.AlbumTitle
                $timeline = $session.GetTimelineProperties()
                $duration = [Math]::Max(0.0, $timeline.EndTime.TotalSeconds - $timeline.StartTime.TotalSeconds)
                $position = [Math]::Max(0.0, $timeline.Position.TotalSeconds - $timeline.StartTime.TotalSeconds)
                $status = [string]$session.GetPlaybackInfo().PlaybackStatus
                $trackKey = [string]$session.SourceAppUserModelId + '|' + $album + '|' + $title + '|' + $artist
                $active = -not [string]::IsNullOrWhiteSpace($title) -or -not [string]::IsNullOrWhiteSpace($artist)

                if ($trackKey -ne $lastTrackKey) {
                    $lastArtworkKey = ''
                    $lastArtworkAttempt = [DateTime]::MinValue
                    $artworkAttempts = 0
                }

                $snapshot.active = $active
                $snapshot.title = $title
                $snapshot.artist = $artist
                $snapshot.trackKey = $trackKey
                $snapshot.playing = $status -eq 'Playing'
                $snapshot.position = $position
                $snapshot.duration = $duration
                try {
                    $snapshot.updatedAt = ([DateTimeOffset]$timeline.LastUpdatedTime).ToUnixTimeMilliseconds()
                } catch {
                    $snapshot.updatedAt = 0
                }

                $now = [DateTime]::UtcNow
                if ($active -and $trackKey -ne $lastArtworkKey -and
                    $artworkAttempts -lt $maximumArtworkAttempts -and
                    ($now - $lastArtworkAttempt).TotalSeconds -ge 1) {
                    $lastArtworkAttempt = $now
                    $artworkAttempts++
                    try {
                        if ($null -eq $properties.Thumbnail) { throw 'Media artwork is not available yet' }
                        $stream = Wait-WinRtOperation ($properties.Thumbnail.OpenReadAsync()) $streamType
                        $reader = [Activator]::CreateInstance($readerType, [object[]]@($stream))
                        $byteCount = Wait-WinRtOperation ($reader.LoadAsync([uint32]1048576)) ([uint32])
                        if ($byteCount -gt 0) {
                            $bytes = [byte[]]::new([int]$byteCount)
                            $reader.ReadBytes($bytes)
                            if (Test-BraveLogoThumbnail $bytes ([string]$session.SourceAppUserModelId)) {
                                $snapshot.cover = ''
                            } else {
                                $snapshot.cover = [Convert]::ToBase64String($bytes)
                                $lastArtworkKey = $trackKey
                            }
                        }
                    } catch {
                        $snapshot.cover = ''
                    }
                }
                $lastTrackKey = $trackKey
            } else {
                $lastTrackKey = ''
                $lastArtworkKey = ''
                $lastArtworkAttempt = [DateTime]::MinValue
            }
        } catch {
            $lastTrackKey = ''
        }

        $line = ConvertTo-Json -InputObject ([pscustomobject]$snapshot) -Compress -Depth 3
        [Console]::Out.WriteLine($line)
        [Console]::Out.Flush()
        Start-Sleep -Milliseconds 500
    }
} catch {
    exit 0
}
