# Media Assets & Demonstration Videos

This directory stores demonstration video clips and poster images for the TrustPay Guardian landing page.

> [!NOTE]
> In accordance with project engineering policies, fake video binaries are not generated into the repository.
> If video files are missing, the landing page automatically displays responsive CSS gradients and poster fallbacks with zero console errors.

---

## Required Media Specifications

| File Name | Description | Recommended Duration | Resolution | Max Size | Codec |
|---|---|---|---|---|---|
| `hero.mp4` | Looping muted background video showcasing digital transaction flow | 15–30 sec | 1280x720 | 8 MB | H.264 / AAC |
| `hero-poster.jpg` | Fallback poster image displayed while video loads or if video is missing | N/A | 1280x720 | 500 KB | JPEG / WebP |
| `how-it-works.mp4` | Walkthrough of transaction evaluation and rule triggers | 20–45 sec | 1280x720 | 8 MB | H.264 / AAC |
| `how-it-works-poster.jpg` | Thumbnail poster for How-It-Works video card | N/A | 1280x720 | 500 KB | JPEG / WebP |
| `security.mp4` | Demonstration of ascending-ID row locks and rate limiting | 15–30 sec | 1280x720 | 8 MB | H.264 / AAC |
| `security-poster.jpg` | Thumbnail poster for Security Architecture video card | N/A | 1280x720 | 500 KB | JPEG / WebP |
| `fraud-demo.mp4` | Walkthrough of analyst alert triage and forensic rule resolution | 20–45 sec | 1280x720 | 8 MB | H.264 / AAC |
| `fraud-demo-poster.jpg` | Thumbnail poster for Fraud Analyst Console video card | N/A | 1280x720 | 500 KB | JPEG / WebP |

---

## Video Acquisition & Licensing Instructions

When recording or acquiring stock footage for these videos:
1. Ensure clips are royalty-free (e.g. from Pexels, Pixabay, or recorded directly from browser testing).
2. Compress footage using HandBrake or ffmpeg:
   ```bash
   ffmpeg -i input.mov -vcodec libx264 -crf 24 -preset slow -acodec aac -b:a 128k -movflags +faststart output.mp4
   ```
3. Record the source and license of any external clip in this table:
   - *Example: `hero.mp4` - Recorded from browser demo - MIT License*
