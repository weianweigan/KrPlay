import React, { useEffect, useState } from 'react';
import { Language, ReleaseInfo } from './types';
import { fetchLatestRelease } from './services/releaseService';
import { ThemeProvider } from './context/ThemeContext';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { Architecture } from './components/Architecture';
import { Features } from './components/Features';
import { WorkflowGuide } from './components/WorkflowGuide';
import { DownloadSection } from './components/DownloadSection';
import { ComplianceSection } from './components/ComplianceSection';
import { Footer } from './components/Footer';

export const AppContent: React.FC = () => {
  const [lang, setLang] = useState<Language>('zh');
  const [release, setRelease] = useState<ReleaseInfo>({
    tag: 'v0.4.2',
    name: 'KrPlay v0.4.2',
    publishedAt: '2026-10-19',
    htmlUrl: 'https://github.com/weianweigan/KrPlay/releases',
    body: '',
    assets: {
      receiverApk: {
        name: 'KrPlay-debug.apk',
        size: 16800000,
        downloadUrl: 'https://github.com/weianweigan/KrPlay/releases/latest/download/KrPlay-debug.apk',
      },
      clientApk: {
        name: 'KrPlay-Client-debug.apk',
        size: 12400000,
        downloadUrl: 'https://github.com/weianweigan/KrPlay/releases/latest/download/KrPlay-Client-debug.apk',
      },
      sourceZip: {
        name: 'KrPlay-source.zip',
        size: 4500000,
        downloadUrl: 'https://github.com/weianweigan/KrPlay/releases/latest/download/KrPlay-source.zip',
      },
      licensesZip: {
        name: 'KrPlay-licenses.zip',
        size: 210000,
        downloadUrl: 'https://github.com/weianweigan/KrPlay/releases/latest/download/KrPlay-licenses.zip',
      },
      all: [],
    },
    isLive: false,
  });

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
    <div className="min-h-screen bg-slate-950 text-slate-100 dark:bg-slate-950 dark:text-slate-100 light:bg-slate-50 light:text-slate-900 transition-colors duration-200">
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
