import React, { useEffect, useState } from 'react';
import { Language, ReleaseInfo } from './types';
import { fetchLatestRelease, FALLBACK_RELEASE } from './services/releaseService';
import { ThemeProvider } from './context/ThemeContext';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { Architecture } from './components/Architecture';
import { Features } from './components/Features';
import { WorkflowGuide } from './components/WorkflowGuide';
import { DownloadSection } from './components/DownloadSection';
import { ComplianceSection } from './components/ComplianceSection';
import { Footer } from './components/Footer';
import { CockpitDock } from './components/CockpitDock';

export const AppContent: React.FC = () => {
  const [lang, setLang] = useState<Language>('zh');
  const [release, setRelease] = useState<ReleaseInfo>(FALLBACK_RELEASE);

  useEffect(() => {
    let mounted = true;
    fetchLatestRelease().then(info => {
      if (mounted) {
        setRelease(info);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div className="min-h-screen bg-[var(--surface-canvas)] text-[var(--text-primary)] transition-colors duration-300 relative pb-16 selection:bg-[#3B5BDB] selection:text-white">
      <Navbar lang={lang} setLang={setLang} />
      <main>
        <Hero lang={lang} release={release} />
        <Architecture lang={lang} />
        <Features lang={lang} />
        <WorkflowGuide lang={lang} />
        <DownloadSection lang={lang} release={release} />
        <ComplianceSection lang={lang} />
      </main>
      <Footer lang={lang} />
      <CockpitDock />
    </div>
  );
};

export default function App() {
  return (
    <ThemeProvider>
      <AppContent />
    </ThemeProvider>
  );
}
