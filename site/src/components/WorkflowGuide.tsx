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
    <section id="guide" className="py-24 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3 mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full zeekr-pill text-xs font-semibold text-[#3B5BDB] dark:text-[#5C7CFA] border border-[#3B5BDB]/25">
            <Compass className="w-3.5 h-3.5 text-[#3B5BDB]" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-[var(--text-primary)]">
            {t.title}
          </h2>
          <p className="text-sm sm:text-base text-[var(--text-secondary)] max-w-2xl mx-auto">
            {t.subtitle}
          </p>
        </div>

        {/* Steps Grid - Zeekr Bento Style */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {t.steps.map((step, idx) => (
            <div
              key={idx}
              className="zeekr-card !p-6 !rounded-[22px] relative flex flex-col justify-between border border-white/60 dark:border-white/10 zeekr-card-hover space-y-6"
            >
              <div>
                {/* Big Display Numeral (Design.md 3.1 Hero Display 44px) */}
                <div className="text-4xl font-black font-mono tracking-tighter text-[#3B5BDB]/30 dark:text-[#5C7CFA]/30 mb-3 select-none">
                  {step.num}
                </div>
                <h3 className="text-base font-bold text-[var(--text-primary)] mb-2">
                  {step.title}
                </h3>
                <p className="text-xs sm:text-sm text-[var(--text-secondary)] leading-relaxed">
                  {step.desc}
                </p>
              </div>

              {idx < t.steps.length - 1 && (
                <div className="hidden lg:block absolute -right-3.5 top-1/2 -translate-y-1/2 z-10 w-7 h-7 rounded-full zeekr-pill flex items-center justify-center text-[var(--text-muted)] shadow-md">
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              )}
            </div>
          ))}
        </div>

        {/* CarPlay Authentication Notice Box (Zeekr Orange Semantic - Design.md 2.3) */}
        <div className="mt-12 p-6 sm:p-7 rounded-[22px] bg-[#F06548]/10 dark:bg-[#F06548]/15 border border-[#F06548]/25 flex flex-col sm:flex-row gap-4 items-start backdrop-blur-xl">
          <div className="p-2.5 rounded-[14px] bg-[#F06548]/20 text-[#D94826] dark:text-[#FF7A59] flex-shrink-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div className="space-y-1.5">
            <h4 className="text-base font-bold text-[#D94826] dark:text-[#FF7A59]">
              {t.noticeTitle}
            </h4>
            <p className="text-xs sm:text-sm text-[var(--text-secondary)] leading-relaxed">
              {t.noticeContent}
            </p>
          </div>
        </div>

      </div>
    </section>
  );
};

