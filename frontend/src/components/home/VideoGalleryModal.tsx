import React, { useState, useRef, useEffect } from 'react';

interface VideoItem {
  id: string;
  title: string;
  duration: string;
  description: string;
  src: string;
  poster: string;
}

const VIDEOS: VideoItem[] = [
  {
    id: 'how-it-works',
    title: 'How Fraud Detection Works',
    duration: '0:35',
    description: 'Walkthrough of real-time transfer assessment and deterministic strategy scoring.',
    src: '/media/how-it-works.mp4',
    poster: '/media/how-it-works-poster.jpg',
  },
  {
    id: 'security',
    title: 'Security & Concurrency Architecture',
    duration: '0:28',
    description: 'Live demonstration of ascending-ID row locking and rate limiting defenses.',
    src: '/media/security.mp4',
    poster: '/media/security-poster.jpg',
  },
  {
    id: 'fraud-demo',
    title: 'Analyst Alert Triage Walkthrough',
    duration: '0:42',
    description: 'Forensic inspection of triggered rule hits, decision notes, and release/block workflows.',
    src: '/media/fraud-demo.mp4',
    poster: '/media/fraud-demo-poster.jpg',
  },
];

export const VideoGalleryModal: React.FC = () => {
  const [activeVideo, setActiveVideo] = useState<VideoItem | null>(null);
  const [videoLoadError, setVideoLoadError] = useState(false);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const closeBtnRef = useRef<HTMLButtonElement | null>(null);

  const openModal = (video: VideoItem, buttonElement: HTMLButtonElement) => {
    triggerRef.current = buttonElement;
    setVideoLoadError(false);
    setActiveVideo(video);
  };

  const closeModal = () => {
    setActiveVideo(null);
    setVideoLoadError(false);
    // Return focus to triggering button for WCAG accessibility
    setTimeout(() => {
      triggerRef.current?.focus();
    }, 50);
  };

  useEffect(() => {
    if (activeVideo) {
      closeBtnRef.current?.focus();

      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === 'Escape') {
          closeModal();
        }
      };
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [activeVideo]);

  return (
    <section className="py-5 py-lg-6 border-bottom" style={{ backgroundColor: 'var(--tpg-bg-surface)', borderColor: 'var(--tpg-border-color)' }}>
      <div className="container">
        <div className="text-center mb-5">
          <span className="badge bg-primary-subtle text-primary mb-2 px-3 py-1 fw-semibold">
            Video Demonstrations
          </span>
          <h2 className="display-6 fw-bold mb-3">Architecture & Feature Walkthroughs</h2>
          <p className="text-muted mx-auto" style={{ maxWidth: '600px' }}>
            Watch short video demonstrations of key fraud workflows, concurrency safety, and analyst operations.
          </p>
        </div>

        <div className="row g-4 justify-content-center">
          {VIDEOS.map((video) => (
            <div key={video.id} className="col-12 col-md-6 col-lg-4">
              <div className="card h-100 tpg-card tpg-card-hover overflow-hidden border">
                {/* Poster / Thumbnail with Play overlay */}
                <div className="position-relative bg-dark" style={{ height: '200px' }}>
                  <img
                    src={video.poster}
                    alt={`${video.title} poster thumbnail`}
                    className="w-100 h-100 object-fit-cover opacity-75"
                    onError={(e) => {
                      // Fallback gradient if poster file is missing
                      e.currentTarget.style.display = 'none';
                    }}
                  />
                  <div
                    className="position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-between p-3"
                    style={{ background: 'linear-gradient(to top, rgba(0,0,0,0.85) 0%, transparent 60%)' }}
                  >
                    <span className="badge bg-dark bg-opacity-75 align-self-end small">
                      {video.duration}
                    </span>
                    <button
                      type="button"
                      onClick={(e) => openModal(video, e.currentTarget)}
                      className="btn btn-primary rounded-circle align-self-center p-3 shadow-lg d-flex align-items-center justify-content-center"
                      style={{ width: '56px', height: '56px' }}
                      aria-label={`Play video: ${video.title}`}
                    >
                      <i className="bi bi-play-fill fs-3 ms-1"></i>
                    </button>
                    <div></div>
                  </div>
                </div>

                <div className="card-body p-4">
                  <h3 className="h6 fw-bold mb-2">{video.title}</h3>
                  <p className="text-muted small mb-0">{video.description}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Accessible Video Modal Player */}
      {activeVideo && (
        <div
          className="modal show d-block"
          tabIndex={-1}
          role="dialog"
          aria-modal="true"
          aria-labelledby="videoModalTitle"
          style={{ backgroundColor: 'rgba(0, 0, 0, 0.75)', zIndex: 1060 }}
        >
          <div className="modal-dialog modal-dialog-centered modal-lg" role="document">
            <div className="modal-content bg-dark text-white border-0 shadow-lg overflow-hidden">
              <div className="modal-header border-bottom border-secondary">
                <h5 className="modal-title fw-bold" id="videoModalTitle">
                  {activeVideo.title}
                </h5>
                <button
                  ref={closeBtnRef}
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close video player"
                  onClick={closeModal}
                ></button>
              </div>

              <div className="modal-body p-0 position-relative bg-black" style={{ minHeight: '360px' }}>
                {!videoLoadError ? (
                  <video
                    controls
                    autoPlay
                    playsInline
                    className="w-100 h-100"
                    poster={activeVideo.poster}
                    onError={() => setVideoLoadError(true)}
                  >
                    <source src={activeVideo.src} type="video/mp4" />
                    Your browser does not support HTML5 video.
                  </video>
                ) : (
                  <div className="p-5 text-center text-white-50">
                    <i className="bi bi-film fs-1 text-primary d-block mb-3"></i>
                    <h6 className="text-white fw-bold mb-2">Simulated Demonstration Video</h6>
                    <p className="small mx-auto mb-3" style={{ maxWidth: '440px' }}>
                      Video file (<code>{activeVideo.src}</code>) is ready for manual recording.
                      Follow the specifications in <code>frontend/public/media/README.md</code> to record or attach footage.
                    </p>
                    <div className="badge bg-secondary">Poster Fallback Active</div>
                  </div>
                )}
              </div>

              <div className="modal-footer border-top border-secondary py-2">
                <span className="small text-muted me-auto">{activeVideo.description}</span>
                <button type="button" className="btn btn-outline-light btn-sm" onClick={closeModal}>
                  Close (Esc)
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
};
