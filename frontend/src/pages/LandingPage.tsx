import React from 'react';
import { PublicNavbar } from '../components/layout/PublicNavbar';
import { HeroSection } from '../components/home/HeroSection';
import { PublicStatsCounters } from '../components/home/PublicStatsCounters';
import { FraudPipelineSection } from '../components/home/FraudPipelineSection';
import { FeatureCardsSection } from '../components/home/FeatureCardsSection';
import { SecurityHighlightsSection } from '../components/home/SecurityHighlightsSection';
import { VideoGalleryModal } from '../components/home/VideoGalleryModal';
import { TestimonialCarousel } from '../components/home/TestimonialCarousel';
import { FaqAccordion } from '../components/home/FaqAccordion';
import { FinalCtaSection } from '../components/home/FinalCtaSection';
import { Footer } from '../components/layout/Footer';

export const LandingPage: React.FC = () => {
  return (
    <div className="d-flex flex-column min-vh-100" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
      {/* 1. Sticky Public Navbar */}
      <PublicNavbar />

      {/* 2. Hero Section with Background Video, Poster & Rotating Headline */}
      <HeroSection />

      {/* 3. Animated Public Stats Counters */}
      <PublicStatsCounters />

      {/* 4. How Fraud Detection Works Pipeline */}
      <FraudPipelineSection />

      {/* 5. Feature Pillars Cards */}
      <FeatureCardsSection />

      {/* 6. Defense-in-Depth Security Highlights */}
      <SecurityHighlightsSection />

      {/* 7. Video Demonstrations Gallery & Modal Player */}
      <VideoGalleryModal />

      {/* 8. Peer Reviews (Labelled as Educational Demo Content) */}
      <TestimonialCarousel />

      {/* 9. Technical & Architecture FAQ Accordion */}
      <FaqAccordion />

      {/* 10. Final Call to Action */}
      <FinalCtaSection />

      {/* 11. Footer */}
      <Footer />
    </div>
  );
};
