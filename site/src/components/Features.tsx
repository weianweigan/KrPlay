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
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-sky-500/10 text-sky-400 border border-sky-500/20">
            <Sparkles className="w-3.5 h-3.5" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
            {t.title}
          </h2>
          <p className="text-sm sm:text-base text-slate-400 max-w-2xl mx-auto">
            {t.subtitle}
          </p>
        </div>

        {/* Feature Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {t.items.map((item, index) => {
            const Icon = FEATURE_ICONS[index % FEATURE_ICONS.length];
            return (
              <div
                key={index}
                className="group relative glass-panel rounded-2xl p-6 hover:border-emerald-500/50 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-emerald-500/10 space-y-4"
              >
                {/* Glow pill on hover */}
                <div className="flex items-center justify-between">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700/80 flex items-center justify-center text-emerald-400 group-hover:text-teal-300 group-hover:scale-110 transition-all">
                    <Icon className="w-6 h-6" />
                  </div>
                  <span className="text-[11px] font-semibold tracking-wider uppercase px-2.5 py-0.5 rounded-full bg-slate-800/80 text-slate-300 border border-slate-700/50">
                    {item.tag}
                  </span>
                </div>

                <h3 className="text-lg font-bold text-slate-100 group-hover:text-emerald-400 transition-colors">
                  {item.title}
                </h3>

                <p className="text-sm text-slate-400 leading-relaxed">
                  {item.description}
                </p>
              </div>
            );
          })}
        </div>

      </div>
    </section>
  );
};
