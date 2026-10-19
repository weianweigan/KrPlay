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
    <section id="compliance" className="py-20 relative bg-slate-900/40 border-t border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3 mb-14">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            <Scale className="w-3.5 h-3.5" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl font-extrabold tracking-tight text-slate-100">
            {t.title}
          </h2>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Left Column: License Compliance */}
          <div className="lg:col-span-7 space-y-6">
            <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
              <div className="flex items-center gap-2.5 text-emerald-400 font-bold text-base">
                <ShieldCheck className="w-5 h-5" />
                <span>GPL-3.0 & AGPL-3.0 协议规范</span>
              </div>
              <p className="text-sm text-slate-300 leading-relaxed">
                {t.content1}
              </p>
            </div>

            {/* Upstream Credits */}
            <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
              <div className="flex items-center gap-2.5 text-sky-400 font-bold text-base">
                <Heart className="w-5 h-5 text-rose-500" />
                <span>致敬开源上游先行者</span>
              </div>
              <p className="text-sm text-slate-400">
                {t.content2}
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
                {t.upstream.map((proj, idx) => (
                  <a
                    key={idx}
                    href={proj.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex flex-col p-3 rounded-xl bg-slate-900/90 border border-slate-800 hover:border-slate-700 hover:bg-slate-800/80 transition-all group"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-slate-200 text-sm group-hover:text-emerald-400">
                        {proj.name}
                      </span>
                      <ExternalLink className="w-3.5 h-3.5 text-slate-500 group-hover:text-slate-300" />
                    </div>
                    <span className="text-xs text-slate-500 mt-1">@{proj.author}</span>
                  </a>
                ))}
              </div>
            </div>
          </div>

          {/* Right Column: Trademark & Disclaimer */}
          <div className="lg:col-span-5">
            <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
              <h3 className="text-base font-bold text-slate-200">
                {t.disclaimerTitle}
              </h3>
              <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
                {t.disclaimer}
              </p>
              <div className="pt-2 text-[11px] text-slate-500 font-mono border-t border-slate-800/80">
                KrPlay is an independent open-source project. Not endorsed by Apple Inc. or Zeekr.
              </div>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
};
