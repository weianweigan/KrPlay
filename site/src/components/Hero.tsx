import React from 'react';
import { Download, Sparkles, ShieldCheck, Wifi, Activity } from 'lucide-react';
import { Language, ReleaseInfo } from '../types';
import { translations } from '../i18n/translations';

interface HeroProps {
  lang: Language;
  release: ReleaseInfo;
}

export const Hero: React.FC<HeroProps> = ({ lang, release }) => {
  const t = translations[lang].hero;

  return (
    <section className="relative overflow-hidden pt-12 pb-20 md:pt-20 md:pb-32">
      {/* Background radial gradients for automotive cockpit atmosphere */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[300px] bg-gradient-to-tr from-emerald-500/20 via-teal-500/10 to-sky-500/20 blur-[120px] pointer-events-none -z-10 rounded-full" />
      <div className="absolute top-10 right-10 w-72 h-72 bg-sky-500/10 blur-[100px] pointer-events-none -z-10 rounded-full" />
      <div className="absolute top-20 left-10 w-72 h-72 bg-emerald-500/10 blur-[100px] pointer-events-none -z-10 rounded-full" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          
          {/* Left Column: Heading & CTAs */}
          <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
            {/* Tag Badge */}
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-emerald-500/10 dark:bg-emerald-500/10 light:bg-emerald-100 border border-emerald-500/30 text-emerald-400 dark:text-emerald-400 light:text-emerald-700 text-xs font-semibold tracking-wide">
              <Sparkles className="w-3.5 h-3.5" />
              <span>{t.badge}</span>
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              <span className="text-slate-400 font-mono">{release.tag}</span>
            </div>

            {/* Main Title */}
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-[1.15]">
              <span className="block text-slate-100 dark:text-slate-100 light:text-slate-900">
                {t.tagline}
              </span>
              <span className="block bg-gradient-to-r from-emerald-400 via-teal-300 to-sky-400 bg-clip-text text-transparent">
                {t.highlightTitle}
              </span>
            </h1>

            {/* Description */}
            <p className="text-base sm:text-lg text-slate-300 dark:text-slate-300 light:text-slate-600 max-w-2xl leading-relaxed mx-auto lg:mx-0">
              {t.description}
            </p>

            {/* Feature Pills */}
            <div className="grid grid-cols-2 sm:grid-cols-2 gap-2.5 pt-2 max-w-xl mx-auto lg:mx-0 text-left">
              {t.featuresList.map((feat, idx) => (
                <div
                  key={idx}
                  className="flex items-center gap-2 text-xs sm:text-sm font-medium text-slate-300 dark:text-slate-300 light:text-slate-700 bg-slate-900/60 dark:bg-slate-900/60 light:bg-slate-100 px-3 py-2 rounded-lg border border-slate-800/80 dark:border-slate-800/80 light:border-slate-200"
                >
                  <ShieldCheck className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                  <span>{feat}</span>
                </div>
              ))}
            </div>

            {/* CTA Buttons */}
            <div className="pt-4 flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
              {/* Receiver Button */}
              <a
                href={release.assets.receiverApk?.downloadUrl || '#downloads'}
                className="group relative flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold shadow-lg shadow-emerald-500/25 transition-all hover:scale-[1.02] active:scale-[0.98]"
              >
                <div className="w-7 h-7 rounded-lg bg-emerald-950/60 flex items-center justify-center border border-emerald-400/30">
                  <img src="./krplay.svg" alt="KrPlay" className="w-4 h-4" />
                </div>
                <div className="text-left">
                  <div className="text-sm font-bold leading-tight">{t.downloadReceiver}</div>
                  <div className="text-[11px] text-emerald-100/80 font-normal">{t.receiverDesc}</div>
                </div>
                <Download className="w-4 h-4 ml-1 group-hover:translate-y-0.5 transition-transform" />
              </a>

              {/* Client Button */}
              <a
                href={release.assets.clientApk?.downloadUrl || '#downloads'}
                className="group relative flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl bg-gradient-to-r from-sky-600 to-blue-600 hover:from-sky-500 hover:to-blue-500 text-white font-bold shadow-lg shadow-sky-500/25 transition-all hover:scale-[1.02] active:scale-[0.98]"
              >
                <div className="w-7 h-7 rounded-lg bg-sky-950/60 flex items-center justify-center border border-sky-400/30">
                  <img src="./krplay-client.svg" alt="KrPlay-Client" className="w-4 h-4" />
                </div>
                <div className="text-left">
                  <div className="text-sm font-bold leading-tight">{t.downloadClient}</div>
                  <div className="text-[11px] text-sky-100/80 font-normal">{t.clientDesc}</div>
                </div>
                <Download className="w-4 h-4 ml-1 group-hover:translate-y-0.5 transition-transform" />
              </a>
            </div>
          </div>

          {/* Right Column: Visual Cockpit & Dual-Device Showcase */}
          <div className="lg:col-span-5 relative flex justify-center">
            <div className="relative w-full max-w-md">
              {/* Glow backdrop */}
              <div className="absolute inset-0 bg-gradient-to-r from-emerald-500/20 to-sky-500/20 rounded-3xl blur-2xl -z-10" />

              {/* Main Cockpit Frame Card */}
              <div className="glass-panel rounded-2xl p-5 shadow-2xl border border-slate-700/60 space-y-4">
                {/* Cockpit Status Bar Header */}
                <div className="flex items-center justify-between pb-3 border-b border-slate-800 text-xs font-mono text-slate-400">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
                    <span className="text-emerald-400 font-semibold">KRPLAY LINK ACTIVE</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="flex items-center gap-1">
                      <Wifi className="w-3.5 h-3.5 text-sky-400" /> 5GHz
                    </span>
                    <span className="flex items-center gap-1 text-emerald-400">
                      <Activity className="w-3.5 h-3.5" /> 8ms
                    </span>
                  </div>
                </div>

                {/* Car Screen Simulator Preview */}
                <div className="relative rounded-xl overflow-hidden bg-slate-900 border border-slate-800 aspect-[16/10] flex flex-col justify-between p-3.5 bg-gradient-to-br from-slate-900 via-slate-950 to-slate-900">
                  {/* CarPlay UI Simulator */}
                  <div className="flex items-start justify-between">
                    <div className="space-y-1">
                      <div className="text-xs font-semibold text-slate-200">Apple CarPlay</div>
                      <div className="text-[10px] text-slate-400 font-mono">1920 × 1080 @ 60 FPS</div>
                    </div>
                    <div className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 text-[10px] font-mono border border-emerald-500/30">
                      H.264 HardDec
                    </div>
                  </div>

                  {/* CarPlay App Grid Mockup */}
                  <div className="grid grid-cols-4 gap-2.5 my-auto px-2">
                    {[
                      { name: '地图', color: 'from-amber-500 to-orange-500', icon: '🗺️' },
                      { name: '音乐', color: 'from-rose-500 to-red-500', icon: '🎵' },
                      { name: '播客', color: 'from-purple-500 to-indigo-500', icon: '🎙️' },
                      { name: '通话', color: 'from-emerald-500 to-teal-500', icon: '📞' },
                    ].map((app, i) => (
                      <div key={i} className="flex flex-col items-center gap-1">
                        <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${app.color} flex items-center justify-center text-base shadow-md`}>
                          {app.icon}
                        </div>
                        <span className="text-[10px] text-slate-300">{app.name}</span>
                      </div>
                    ))}
                  </div>

                  {/* Collapsible Sidebar Dock Preview */}
                  <div className="flex items-center justify-between pt-2 border-t border-slate-800/80 text-[11px] text-slate-400">
                    <span className="flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-sky-400" />
                      极氪 8295 专属悬浮侧栏
                    </span>
                    <span className="text-slate-400 font-mono">12:35</span>
                  </div>
                </div>

                {/* Dual-device telemetry pills */}
                <div className="grid grid-cols-2 gap-3 pt-1">
                  {/* Receiver device */}
                  <div className="p-3 rounded-xl bg-emerald-950/30 border border-emerald-500/20 flex items-center gap-2.5">
                    <img src="./krplay.svg" alt="KrPlay" className="w-6 h-6 flex-shrink-0" />
                    <div className="min-w-0">
                      <div className="text-xs font-bold text-emerald-300 truncate">接收端 (KrPlay)</div>
                      <div className="text-[10px] text-slate-400 truncate">热点 / USB 直连 iPhone</div>
                    </div>
                  </div>

                  {/* Client device */}
                  <div className="p-3 rounded-xl bg-sky-950/30 border border-sky-500/20 flex items-center gap-2.5">
                    <img src="./krplay-client.svg" alt="KrPlay-Client" className="w-6 h-6 flex-shrink-0" />
                    <div className="min-w-0">
                      <div className="text-xs font-bold text-sky-300 truncate">车机端 (Client)</div>
                      <div className="text-[10px] text-slate-400 truncate">车机屏幕与触控输出</div>
                    </div>
                  </div>
                </div>

              </div>
            </div>
          </div>

        </div>
      </div>
    </section>
  );
};
