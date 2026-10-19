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
      {/* Background radial glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[350px] bg-gradient-to-r from-emerald-500/10 via-sky-500/10 to-teal-500/10 blur-[140px] pointer-events-none -z-10 rounded-full" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3 mb-12">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <Sparkles className="w-3.5 h-3.5" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
            {t.title}
          </h2>
          <p className="text-sm sm:text-base text-slate-400 max-w-2xl mx-auto">
            {t.subtitle}
          </p>

          {/* Current Tag Info Badge */}
          <div className="pt-2 flex flex-wrap items-center justify-center gap-3 text-xs">
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700/80 font-mono">
              <span className="text-slate-400">{t.currentVersion}:</span>
              <span className="font-bold text-emerald-400">{release.tag}</span>
              <span className="text-slate-500">({release.publishedAt})</span>
            </div>

            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700/80 text-slate-300">
              <span className={`w-2 h-2 rounded-full ${release.isLive ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`} />
              <span>{release.isLive ? t.liveTag : t.fallbackTag}</span>
            </div>
          </div>
        </div>

        {/* 4 Download Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          
          {/* 1. KrPlay Receiver APK */}
          <div className="glass-panel rounded-2xl p-6 flex flex-col justify-between border-t-4 border-t-emerald-500 hover:shadow-xl hover:shadow-emerald-500/10 transition-all">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-xl bg-emerald-500/20 p-2.5 flex items-center justify-center border border-emerald-500/30">
                <img src="./krplay.svg" alt="KrPlay" className="w-full h-full object-contain" />
              </div>
              <h3 className="text-lg font-bold text-slate-100">
                {t.cards.receiver.title}
              </h3>
              <div className="text-xs text-emerald-400 font-medium">
                {t.cards.receiver.target}
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                {t.cards.receiver.desc}
              </p>
            </div>

            <div className="pt-6 space-y-3">
              <div className="flex items-center justify-between text-xs font-mono text-slate-400">
                <span className="truncate max-w-[140px]">{release.assets.receiverApk?.name || t.cards.receiver.filename}</span>
                <span>{release.assets.receiverApk ? formatBytes(release.assets.receiverApk.size) : '16.8 MB'}</span>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={release.assets.receiverApk?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs transition-colors shadow-md shadow-emerald-600/30"
                >
                  <Download className="w-4 h-4" />
                  <span>{t.cards.receiver.btn}</span>
                </a>
                <button
                  onClick={() => handleCopyLink('receiver', release.assets.receiverApk?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`)}
                  title="复制直链 / Copy Link"
                  className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                >
                  {copiedName === 'receiver' ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </div>

          {/* 2. KrPlay-Client Car APK */}
          <div className="glass-panel rounded-2xl p-6 flex flex-col justify-between border-t-4 border-t-sky-500 hover:shadow-xl hover:shadow-sky-500/10 transition-all">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-xl bg-sky-500/20 p-2.5 flex items-center justify-center border border-sky-500/30">
                <img src="./krplay-client.svg" alt="KrPlay-Client" className="w-full h-full object-contain" />
              </div>
              <h3 className="text-lg font-bold text-slate-100">
                {t.cards.client.title}
              </h3>
              <div className="text-xs text-sky-400 font-medium">
                {t.cards.client.target}
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                {t.cards.client.desc}
              </p>
            </div>

            <div className="pt-6 space-y-3">
              <div className="flex items-center justify-between text-xs font-mono text-slate-400">
                <span className="truncate max-w-[140px]">{release.assets.clientApk?.name || t.cards.client.filename}</span>
                <span>{release.assets.clientApk ? formatBytes(release.assets.clientApk.size) : '12.4 MB'}</span>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={release.assets.clientApk?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-semibold text-xs transition-colors shadow-md shadow-sky-600/30"
                >
                  <Download className="w-4 h-4" />
                  <span>{t.cards.client.btn}</span>
                </a>
                <button
                  onClick={() => handleCopyLink('client', release.assets.clientApk?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`)}
                  title="复制直链 / Copy Link"
                  className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                >
                  {copiedName === 'client' ? <Check className="w-4 h-4 text-sky-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </div>

          {/* 3. Source ZIP */}
          <div className="glass-panel rounded-2xl p-6 flex flex-col justify-between border-t-4 border-t-teal-500 hover:shadow-xl hover:shadow-teal-500/10 transition-all">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-xl bg-teal-500/20 p-2.5 flex items-center justify-center border border-teal-500/30 text-teal-400">
                <FolderArchive className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-100">
                {t.cards.source.title}
              </h3>
              <div className="text-xs text-teal-400 font-medium">
                {t.cards.source.target}
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                {t.cards.source.desc}
              </p>
            </div>

            <div className="pt-6 space-y-3">
              <div className="flex items-center justify-between text-xs font-mono text-slate-400">
                <span className="truncate max-w-[140px]">{release.assets.sourceZip?.name || t.cards.source.filename}</span>
                <span>{release.assets.sourceZip ? formatBytes(release.assets.sourceZip.size) : '4.5 MB'}</span>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={release.assets.sourceZip?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-teal-700 hover:bg-teal-600 text-white font-semibold text-xs transition-colors"
                >
                  <Download className="w-4 h-4" />
                  <span>{t.cards.source.btn}</span>
                </a>
                <button
                  onClick={() => handleCopyLink('source', release.assets.sourceZip?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`)}
                  title="复制直链 / Copy Link"
                  className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                >
                  {copiedName === 'source' ? <Check className="w-4 h-4 text-teal-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </div>

          {/* 4. Licenses ZIP */}
          <div className="glass-panel rounded-2xl p-6 flex flex-col justify-between border-t-4 border-t-indigo-500 hover:shadow-xl hover:shadow-indigo-500/10 transition-all">
            <div className="space-y-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-500/20 p-2.5 flex items-center justify-center border border-indigo-500/30 text-indigo-400">
                <FileText className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-100">
                {t.cards.licenses.title}
              </h3>
              <div className="text-xs text-indigo-400 font-medium">
                {t.cards.licenses.target}
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                {t.cards.licenses.desc}
              </p>
            </div>

            <div className="pt-6 space-y-3">
              <div className="flex items-center justify-between text-xs font-mono text-slate-400">
                <span className="truncate max-w-[140px]">{release.assets.licensesZip?.name || t.cards.licenses.filename}</span>
                <span>{release.assets.licensesZip ? formatBytes(release.assets.licensesZip.size) : '210 KB'}</span>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={release.assets.licensesZip?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-indigo-700 hover:bg-indigo-600 text-white font-semibold text-xs transition-colors"
                >
                  <Download className="w-4 h-4" />
                  <span>{t.cards.licenses.btn}</span>
                </a>
                <button
                  onClick={() => handleCopyLink('licenses', release.assets.licensesZip?.downloadUrl || `${GITHUB_RELEASES_URL}/tag/${release.tag}`)}
                  title="复制直链 / Copy Link"
                  className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                >
                  {copiedName === 'licenses' ? <Check className="w-4 h-4 text-indigo-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </div>

        </div>

        {/* GitHub Releases Link Footer Button */}
        <div className="mt-12 text-center">
          <a
            href={GITHUB_RELEASES_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 text-slate-200 text-sm font-semibold transition-all hover:scale-[1.02]"
          >
            <span>{t.viewAllReleases}</span>
            <ExternalLink className="w-4 h-4 text-emerald-400" />
          </a>
        </div>

      </div>
    </section>
  );
};
