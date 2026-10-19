import React from 'react';
import { Compass, AlertTriangle, ArrowRight } from 'lucide-react';
import { Language } from '../types';
import { translations } from '../i18n/translations';

interface WorkflowGuideProps {
  lang: Language;
}

export const WorkflowGuide: React.FC<WorkflowGuideProps> = ({ lang }) => {
  const t = translations[lang].guide;

  return (
    <section id="guide" className="py-24 relative bg-slate-900/30 border-t border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3 mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <Compass className="w-3.5 h-3.5" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
            {t.title}
          </h2>
          <p className="text-sm sm:text-base text-slate-400 max-w-2xl mx-auto">
            {t.subtitle}
          </p>
        </div>

        {/* Steps Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {t.steps.map((step, idx) => (
            <div
              key={idx}
              className="glass-panel rounded-2xl p-6 relative flex flex-col justify-between border border-slate-800 hover:border-slate-700 transition-colors"
            >
              <div>
                <div className="text-3xl font-black font-mono text-emerald-400/40 mb-3">
                  {step.num}
                </div>
                <h3 className="text-base font-bold text-slate-100 mb-2">
                  {step.title}
                </h3>
                <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
                  {step.desc}
                </p>
              </div>

              {idx < t.steps.length - 1 && (
                <div className="hidden lg:block absolute -right-3 top-1/2 -translate-y-1/2 z-10 text-slate-600">
                  <ArrowRight className="w-5 h-5" />
                </div>
              )}
            </div>
          ))}
        </div>

        {/* CarPlay Authentication Materials Notice Box */}
        <div className="mt-12 p-6 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex flex-col sm:flex-row gap-4 items-start">
          <div className="p-2.5 rounded-xl bg-amber-500/20 text-amber-400 flex-shrink-0">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div className="space-y-1.5">
            <h4 className="text-base font-bold text-amber-300">
              {t.noticeTitle}
            </h4>
            <p className="text-xs sm:text-sm text-amber-200/80 leading-relaxed">
              {t.noticeContent}
            </p>
          </div>
        </div>

      </div>
    </section>
  );
};
