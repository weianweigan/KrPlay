import React from 'react';
import { Github } from 'lucide-react';
import { Language } from '../types';
import { translations } from '../i18n/translations';
import { GITHUB_REPO_URL, GITHUB_RELEASES_URL } from '../services/releaseService';

interface FooterProps {
  lang: Language;
}

export const Footer: React.FC<FooterProps> = ({ lang }) => {
  const t = translations[lang].footer;

  return (
    <footer className="border-t border-slate-800 bg-slate-950/80 py-12 text-slate-400 text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          
          {/* Logo & Tagline */}
          <div className="flex items-center gap-3">
            <div className="w-7 h-7 rounded-lg bg-emerald-500/20 p-1 flex items-center justify-center border border-emerald-500/30">
              <img src="./krplay.svg" alt="KrPlay" className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-slate-200">KrPlay</span>
              <span className="mx-2 text-slate-600">·</span>
              <span>{t.desc}</span>
            </div>
          </div>

          {/* Links */}
          <div className="flex flex-wrap items-center gap-6 text-slate-400">
            <a
              href={GITHUB_REPO_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-emerald-400 transition-colors flex items-center gap-1.5"
            >
              <Github className="w-3.5 h-3.5" />
              <span>{t.repo}</span>
            </a>
            <a
              href={GITHUB_RELEASES_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-emerald-400 transition-colors"
            >
              {t.releases}
            </a>
            <a
              href={`${GITHUB_REPO_URL}/blob/main/DISTRIBUTION.md`}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-emerald-400 transition-colors"
            >
              {t.distributionDoc}
            </a>
            <a
              href={`${GITHUB_REPO_URL}/blob/main/LICENSE`}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-emerald-400 transition-colors"
            >
              {t.licenseDoc}
            </a>
          </div>

        </div>

        {/* Copyright notice */}
        <div className="mt-8 pt-6 border-t border-slate-900 text-center text-slate-600 text-[11px]">
          {t.copyright}
        </div>
      </div>
    </footer>
  );
};
