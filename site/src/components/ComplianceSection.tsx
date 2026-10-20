import React from 'react';
import { ShieldCheck, Scale, ExternalLink, Heart } from 'lucide-react';
import { Language } from '../types';
import { translations } from '../i18n/translations';

interface ComplianceSectionProps {
  lang: Language;
}

export const ComplianceSection: React.FC<ComplianceSectionProps> = ({ lang }) => {
  const t = translations[lang].compliance;

  return (
    <section id="compliance" className="py-20 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3 mb-14">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full zeekr-pill text-xs font-semibold text-[#7B52AB] dark:text-[#9A74C9] border border-[#7B52AB]/25">
            <Scale className="w-3.5 h-3.5" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl font-extrabold tracking-tight text-[var(--text-primary)]">
            {t.title}
          </h2>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Left Column: License Compliance */}
          <div className="lg:col-span-7 space-y-6">
            <div className="zeekr-card !p-7 !rounded-[24px] border border-white/60 dark:border-white/10 space-y-4">
              <div className="flex items-center gap-2.5 text-[#2B8A3E] dark:text-[#40C057] font-bold text-base">
                <ShieldCheck className="w-5 h-5" />
                <span>GPL-3.0 & AGPL-3.0 协议规范</span>
              </div>
              <p className="text-sm text-[var(--text-secondary)] leading-relaxed">
                {t.content1}
              </p>
            </div>

            {/* Upstream Credits */}
            <div className="zeekr-card !p-7 !rounded-[24px] border border-white/60 dark:border-white/10 space-y-4">
              <div className="flex items-center gap-2.5 text-[#3B5BDB] dark:text-[#5C7CFA] font-bold text-base">
                <Heart className="w-5 h-5 text-rose-500 fill-rose-500/20" />
                <span>致敬开源上游先行者</span>
              </div>
              <p className="text-sm text-[var(--text-secondary)]">
                {t.content2}
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
                {t.upstream.map((proj, idx) => (
                  <a
                    key={idx}
                    href={proj.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex flex-col p-3.5 rounded-[16px] zeekr-pill border border-white/40 dark:border-white/10 hover:border-[#3B5BDB]/50 transition-all group zeekr-interactive"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-[var(--text-primary)] text-sm group-hover:text-[#3B5BDB] dark:group-hover:text-[#5C7CFA]">
                        {proj.name}
                      </span>
                      <ExternalLink className="w-3.5 h-3.5 text-[var(--text-muted)] group-hover:text-[#3B5BDB]" />
                    </div>
                    <span className="text-xs text-[var(--text-muted)] mt-1 font-mono">@{proj.author}</span>
                  </a>
                ))}
              </div>
            </div>
          </div>

          {/* Right Column: Trademark & Disclaimer */}
          <div className="lg:col-span-5">
            <div className="zeekr-card !p-7 !rounded-[24px] border border-white/60 dark:border-white/10 space-y-4">
              <h3 className="text-base font-bold text-[var(--text-primary)]">
                {t.disclaimerTitle}
              </h3>
              <p className="text-xs sm:text-sm text-[var(--text-secondary)] leading-relaxed">
                {t.disclaimer}
              </p>
              <div className="pt-3 text-[11px] text-[var(--text-muted)] font-mono border-t border-black/5 dark:border-white/5">
                KrPlay is an independent open-source project. Not endorsed by Apple Inc. or Zeekr.
              </div>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
};

