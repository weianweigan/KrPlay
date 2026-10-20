import React, { useState } from 'react';
import { 
  Download, 
  ExternalLink, 
  FileText, 
  Copy, 
  Check, 
  Sparkles, 
  FolderArchive
} from 'lucide-react';
import { Language, ReleaseInfo } from '../types';
import { translations } from '../i18n/translations';
import { formatBytes, GITHUB_RELEASES_URL } from '../services/releaseService';

interface DownloadSectionProps {
  lang: Language;
  release: ReleaseInfo;
}

export const DownloadSection: React.FC<DownloadSectionProps> = ({ lang, release }) => {
  const t = translations[lang].downloads;
  const [copiedName, setCopiedName] = useState<string | null>(null);

  const handleCopyLink = (name: string, url: string) => {
    navigator.clipboard.writeText(url);
    setCopiedName(name);
    setTimeout(() => setCopiedName(null), 2500);
  };

  return (
    <section id="downloads" className="py-24 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3 mb-12">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full zeekr-pill text-xs font-semibold text-[#3B5BDB] dark:text-[#5C7CFA] border border-[#3B5BDB]/25">
            <Sparkles className="w-3.5 h-3.5 text-[#3B5BDB]" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-[var(--text-primary)]">
            {t.title}
          </h2>
          <p className="text-sm sm:text-base text-[var(--text-secondary)] max-w-2xl mx-auto">
            {t.subtitle}
          </p>

          {/* Current Tag Info Badge (Zeekr Pill Capsule) */}
          <div className="pt-2 flex flex-wrap items-center justify-center gap-3 text-xs">
            <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-full zeekr-pill font-mono border border-white/50 dark:border-white/10">
              <span className="text-[var(--text-muted)]">{t.currentVersion}:</span>
              <span className="font-bold text-[#3B5BDB] dark:text-[#5C7CFA]">{release.tag}</span>
              <span className="text-[var(--text-muted)]">({release.publishedAt})</span>
            </div>

            <div className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-full zeekr-pill text-[var(--text-secondary)] border border-white/50 dark:border-white/10">
              <span className={`w-2 h-2 rounded-full ${release.isLive ? 'bg-[#2B8A3E] dark:bg-[#40C057] animate-pulse' : 'bg-amber-400'}`} />
              <span>{release.isLive ? t.liveTag : t.fallbackTag}</span>
            </div>
          </div>
        </div>

        {/* 4 Download Cards Grid - Zeekr Bento Style */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          
          {/* 1. KrPlay Receiver APK */}
          <div className="zeekr-card !p-6 !rounded-[24px] flex flex-col justify-between border-t-4 border-t-[#2B8A3E] zeekr-card-hover zeekr-interactive border border-white/50 dark:border-white/10">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-[14px] bg-[#2B8A3E]/15 p-2.5 flex items-center justify-center border border-[#2B8A3E]/30">
                <img src="./krplay.svg" alt="KrPlay" className="w-full h-full object-contain" />
              </div>
              <h3 className="text-lg font-bold text-[var(--text-primary)]">
                {t.cards.receiver.title}
              </h3>
              <div className="text-xs text-[#2B8A3E] dark:text-[#40C057] font-semibold">
                {t.cards.receiver.target}
              </div>
              <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                {t.cards.receiver.desc}
              </p>
            </div>

            <div className="pt-6 space-y-3">
              <div className="flex items-baseline justify-between text-xs font-mono text-[var(--text-muted)] border-b border-black/5 dark:border-white/5 pb-2">
                <span className="truncate max-w-[130px]">{release.assets.receiverApk?.name || t.cards.receiver.filename}</span>
                <span className="font-semibold text-[var(--text-primary)]">{release.assets.receiverApk ? formatBytes(release.assets.receiverApk.size) : '16.8 MB'}</span>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={release.assets.receiverApk?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-[12px] bg-[#2B8A3E] hover:bg-[#37B24D] text-white font-semibold text-xs transition-colors shadow-sm zeekr-interactive"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>{t.cards.receiver.btn}</span>
                </a>
                <button
                  onClick={() => handleCopyLink('receiver', release.assets.receiverApk?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`)}
                  title="复制直链 / Copy Link"
                  className="p-2.5 rounded-[12px] zeekr-pill text-[var(--text-secondary)] hover:text-[var(--text-primary)] zeekr-interactive"
                >
                  {copiedName === 'receiver' ? <Check className="w-3.5 h-3.5 text-[#2B8A3E] dark:text-[#40C057]" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          </div>

          {/* 2. KrPlay-Client Car APK */}
          <div className="zeekr-card !p-6 !rounded-[24px] flex flex-col justify-between border-t-4 border-t-[#3B5BDB] zeekr-card-hover zeekr-interactive border border-white/50 dark:border-white/10">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-[14px] bg-[#3B5BDB]/15 p-2.5 flex items-center justify-center border border-[#3B5BDB]/30">
                <img src="./krplay-client.svg" alt="KrPlay-Client" className="w-full h-full object-contain" />
              </div>
              <h3 className="text-lg font-bold text-[var(--text-primary)]">
                {t.cards.client.title}
              </h3>
              <div className="text-xs text-[#3B5BDB] dark:text-[#5C7CFA] font-semibold">
                {t.cards.client.target}
              </div>
              <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                {t.cards.client.desc}
              </p>
            </div>

            <div className="pt-6 space-y-3">
              <div className="flex items-baseline justify-between text-xs font-mono text-[var(--text-muted)] border-b border-black/5 dark:border-white/5 pb-2">
                <span className="truncate max-w-[130px]">{release.assets.clientApk?.name || t.cards.client.filename}</span>
                <span className="font-semibold text-[var(--text-primary)]">{release.assets.clientApk ? formatBytes(release.assets.clientApk.size) : '12.4 MB'}</span>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={release.assets.clientApk?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-[12px] bg-[#3B5BDB] hover:bg-[#4C6EF5] text-white font-semibold text-xs transition-colors shadow-sm zeekr-interactive"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>{t.cards.client.btn}</span>
                </a>
                <button
                  onClick={() => handleCopyLink('client', release.assets.clientApk?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`)}
                  title="复制直链 / Copy Link"
                  className="p-2.5 rounded-[12px] zeekr-pill text-[var(--text-secondary)] hover:text-[var(--text-primary)] zeekr-interactive"
                >
                  {copiedName === 'client' ? <Check className="w-3.5 h-3.5 text-[#3B5BDB] dark:text-[#5C7CFA]" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          </div>

          {/* 3. Source ZIP */}
          <div className="zeekr-card !p-6 !rounded-[24px] flex flex-col justify-between border-t-4 border-t-teal-500 zeekr-card-hover zeekr-interactive border border-white/50 dark:border-white/10">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-[14px] bg-teal-500/15 p-2.5 flex items-center justify-center border border-teal-500/30 text-teal-600 dark:text-teal-400">
                <FolderArchive className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-[var(--text-primary)]">
                {t.cards.source.title}
              </h3>
              <div className="text-xs text-teal-600 dark:text-teal-400 font-semibold">
                {t.cards.source.target}
              </div>
              <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                {t.cards.source.desc}
              </p>
            </div>

            <div className="pt-6 space-y-3">
              <div className="flex items-baseline justify-between text-xs font-mono text-[var(--text-muted)] border-b border-black/5 dark:border-white/5 pb-2">
                <span className="truncate max-w-[130px]">{release.assets.sourceZip?.name || t.cards.source.filename}</span>
                <span className="font-semibold text-[var(--text-primary)]">{release.assets.sourceZip ? formatBytes(release.assets.sourceZip.size) : '4.5 MB'}</span>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={release.assets.sourceZip?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-[12px] bg-teal-600 hover:bg-teal-500 text-white font-semibold text-xs transition-colors zeekr-interactive"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>{t.cards.source.btn}</span>
                </a>
                <button
                  onClick={() => handleCopyLink('source', release.assets.sourceZip?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`)}
                  title="复制直链 / Copy Link"
                  className="p-2.5 rounded-[12px] zeekr-pill text-[var(--text-secondary)] hover:text-[var(--text-primary)] zeekr-interactive"
                >
                  {copiedName === 'source' ? <Check className="w-3.5 h-3.5 text-teal-500" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          </div>

          {/* 4. Licenses ZIP */}
          <div className="zeekr-card !p-6 !rounded-[24px] flex flex-col justify-between border-t-4 border-t-[#7B52AB] zeekr-card-hover zeekr-interactive border border-white/50 dark:border-white/10">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-[14px] bg-[#7B52AB]/15 p-2.5 flex items-center justify-center border border-[#7B52AB]/30 text-[#7B52AB] dark:text-[#9A74C9]">
                <FileText className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-[var(--text-primary)]">
                {t.cards.licenses.title}
              </h3>
              <div className="text-xs text-[#7B52AB] dark:text-[#9A74C9] font-semibold">
                {t.cards.licenses.target}
              </div>
              <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
                {t.cards.licenses.desc}
              </p>
            </div>

            <div className="pt-6 space-y-3">
              <div className="flex items-baseline justify-between text-xs font-mono text-[var(--text-muted)] border-b border-black/5 dark:border-white/5 pb-2">
                <span className="truncate max-w-[130px]">{release.assets.licensesZip?.name || t.cards.licenses.filename}</span>
                <span className="font-semibold text-[var(--text-primary)]">{release.assets.licensesZip ? formatBytes(release.assets.licensesZip.size) : '210 KB'}</span>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={release.assets.licensesZip?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-[12px] bg-[#7B52AB] hover:bg-[#8A63BA] text-white font-semibold text-xs transition-colors zeekr-interactive"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>{t.cards.licenses.btn}</span>
                </a>
                <button
                  onClick={() => handleCopyLink('licenses', release.assets.licensesZip?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`)}
                  title="复制直链 / Copy Link"
                  className="p-2.5 rounded-[12px] zeekr-pill text-[var(--text-secondary)] hover:text-[var(--text-primary)] zeekr-interactive"
                >
                  {copiedName === 'licenses' ? <Check className="w-3.5 h-3.5 text-[#7B52AB]" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          </div>

        </div>

        {/* GitHub Releases Link Footer Button (Zeekr Dark CTA - Design.md 2.3) */}
        <div className="mt-12 text-center">
          <a
            href={GITHUB_RELEASES_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-6 py-3 rounded-full bg-[#141517] dark:bg-white text-white dark:text-[#141517] text-xs font-semibold shadow-md transition-all zeekr-interactive"
          >
            <span>{t.viewAllReleases}</span>
            <ExternalLink className="w-3.5 h-3.5 text-[#3B5BDB] dark:text-[#4C6EF5]" />
          </a>
        </div>

      </div>
    </section>
  );
};

