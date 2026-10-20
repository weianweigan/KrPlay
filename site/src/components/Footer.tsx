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
    <footer className="border-t border-black/5 dark:border-white/5 py-12 text-[var(--text-muted)] text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          
          {/* Logo & Tagline */}
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-[12px] bg-gradient-to-br from-[#3B5BDB] to-[#7B52AB] p-0.5 flex items-center justify-center">
              <div className="w-full h-full bg-[#14151A] rounded-[10px] flex items-center justify-center">
                <img src="./krplay.svg" alt="KrPlay" className="w-4 h-4 object-contain" />
              </div>
            </div>
            <div>
              <span className="font-bold text-[var(--text-primary)]">KrPlay</span>
              <span className="mx-2 text-[var(--text-muted)]">·</span>
              <span>{t.desc}</span>
            </div>
          </div>

          {/* Links */}
          <div className="flex flex-wrap items-center gap-6 text-[var(--text-secondary)]">
            <a
              href={GITHUB_REPO_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-[#3B5BDB] dark:hover:text-[#5C7CFA] transition-colors flex items-center gap-1.5"
            >
              <Github className="w-3.5 h-3.5" />
              <span>{t.repo}</span>
            </a>
            <a
              href={GITHUB_RELEASES_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-[#3B5BDB] dark:hover:text-[#5C7CFA] transition-colors"
            >
              {t.releases}
            </a>
            <a
              href={`${GITHUB_REPO_URL}/blob/main/DISTRIBUTION.md`}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-[#3B5BDB] dark:hover:text-[#5C7CFA] transition-colors"
            >
              {t.distributionDoc}
            </a>
            <a
              href={`${GITHUB_REPO_URL}/blob/main/LICENSE`}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-[#3B5BDB] dark:hover:text-[#5C7CFA] transition-colors"
            >
              {t.licenseDoc}
            </a>
          </div>

        </div>

        {/* Copyright notice */}
        <div className="mt-8 pt-6 border-t border-black/5 dark:border-white/5 text-center text-[var(--text-muted)] text-[11px]">
          {t.copyright}
        </div>
      </div>
    </footer>
  );
};

