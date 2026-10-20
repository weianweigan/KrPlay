import React from 'react';
import { 
  SlidersHorizontal, 
  Mic, 
  Radio, 
  Network, 
  Cpu, 
  Smartphone,
  Sparkles 
} from 'lucide-react';
import { Language } from '../types';
import { translations } from '../i18n/translations';

interface FeaturesProps {
  lang: Language;
}

const FEATURE_ICONS = [
  SlidersHorizontal,
  Mic,
  Radio,
  Network,
  Cpu,
  Smartphone,
];

export const Features: React.FC<FeaturesProps> = ({ lang }) => {
  const t = translations[lang].features;

  return (
    <section id="features" className="py-24 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3 mb-16">
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
        </div>

        {/* Feature Cards Bento Grid (Design.md 5.2) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {t.items.map((item, index) => {
            const Icon = FEATURE_ICONS[index % FEATURE_ICONS.length];
            return (
              <div
                key={index}
                className="group relative zeekr-card !p-6 !rounded-[22px] zeekr-card-hover zeekr-interactive border border-white/60 dark:border-white/10 space-y-4 flex flex-col justify-between"
              >
                <div>
                  {/* Top Header with Micro-3D Gradient Icon & Tag */}
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-[16px] bg-gradient-to-br from-[#3B5BDB]/15 via-[#4C6EF5]/10 to-[#7B52AB]/15 dark:from-white/10 dark:to-white/5 border border-white/40 dark:border-white/10 flex items-center justify-center text-[#3B5BDB] dark:text-[#5C7CFA] shadow-sm group-hover:scale-105 transition-transform">
                      <Icon className="w-6 h-6" />
                    </div>
                    {/* Zeekr Tag Badge */}
                    <span className="text-[11px] font-semibold tracking-wider px-3 py-1 rounded-full bg-black/5 dark:bg-white/10 text-[var(--text-secondary)] border border-black/5 dark:border-white/10">
                      {item.tag}
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-[var(--text-primary)] group-hover:text-[#3B5BDB] dark:group-hover:text-[#5C7CFA] transition-colors">
                    {item.title}
                  </h3>

                  <p className="text-sm text-[var(--text-secondary)] leading-relaxed mt-2">
                    {item.description}
                  </p>
                </div>

                {/* Subtle bottom indicator dot */}
                <div className="pt-2 flex items-center gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity text-xs font-semibold text-[#3B5BDB] dark:text-[#5C7CFA]">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#3B5BDB]" />
                  <span>极氪 8295 深度适配</span>
                </div>
              </div>
            );
          })}
        </div>

      </div>
    </section>
  );
};

