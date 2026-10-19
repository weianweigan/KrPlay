import React from 'react';
import { Smartphone, Car, ArrowRight, ShieldCheck, Info, CheckCircle2, Wifi, Zap } from 'lucide-react';
import { Language } from '../types';
import { translations } from '../i18n/translations';

interface ArchitectureProps {
  lang: Language;
}

export const Architecture: React.FC<ArchitectureProps> = ({ lang }) => {
  const t = translations[lang].architecture;

  return (
    <section id="architecture" className="py-20 relative bg-slate-900/40 dark:bg-slate-900/40 light:bg-slate-50 border-y border-slate-800/80">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <Zap className="w-3.5 h-3.5" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
            {t.title}
          </h2>
          <p className="text-sm sm:text-base text-slate-400 max-w-2xl mx-auto">
            {t.subtitle}
          </p>
        </div>

        {/* Network Topology Flow Visual */}
        <div className="mt-12 mb-16 p-6 rounded-2xl glass-panel border border-slate-700/60 shadow-xl">
          <div className="text-xs font-mono font-semibold text-slate-400 uppercase tracking-wider mb-6 flex items-center justify-between">
            <span>连接拓扑示意图 (Connection Topology)</span>
            <span className="text-emerald-400">两级链路 · 独立隔离</span>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
            {/* Stage 1: iPhone */}
            <div className="flex flex-col items-center text-center p-5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-2">
              <div className="w-12 h-12 rounded-2xl bg-slate-800 flex items-center justify-center text-2xl shadow-inner">
                📱
              </div>
              <div className="font-bold text-slate-100">iPhone</div>
              <div className="text-xs text-slate-400">运行 iOS 原生 CarPlay 服务</div>
              <div className="text-[11px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                无线热点 / 有线 USB
              </div>
            </div>

            {/* Stage 2: Receiver Phone */}
            <div className="flex flex-col items-center text-center p-5 rounded-xl bg-emerald-950/20 border border-emerald-500/40 relative space-y-2 shadow-glow-receiver">
              <div className="absolute -top-3 px-2.5 py-0.5 rounded-full bg-emerald-500 text-slate-950 text-[10px] font-black uppercase tracking-wider">
                核心计算枢纽
              </div>
              <div className="w-12 h-12 rounded-2xl bg-emerald-900/50 flex items-center justify-center p-2.5">
                <img src="./krplay.svg" alt="KrPlay" className="w-full h-full object-contain" />
              </div>
              <div className="font-bold text-emerald-300">闲置安卓设备 (KrPlay)</div>
              <div className="text-xs text-slate-400">接收认证握手 · 硬解推流 · 借网 VPN</div>
              <div className="text-[11px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                低延迟 TCP 局域网传输
              </div>
            </div>

            {/* Stage 3: Car Head Unit */}
            <div className="flex flex-col items-center text-center p-5 rounded-xl bg-sky-950/20 border border-sky-500/40 space-y-2 shadow-glow-client">
              <div className="w-12 h-12 rounded-2xl bg-sky-900/50 flex items-center justify-center p-2.5">
                <img src="./krplay-client.svg" alt="KrPlay-Client" className="w-full h-full object-contain" />
              </div>
              <div className="font-bold text-sky-300">车机大屏 (KrPlay-Client)</div>
              <div className="text-xs text-slate-400">极氪 8295 原生渲染 · 触控映射 · 麦克风输入</div>
              <div className="text-[11px] font-mono text-sky-400 bg-sky-500/10 px-2 py-0.5 rounded border border-sky-500/20">
                全屏沉浸驾驶交互
              </div>
            </div>
          </div>
        </div>

        {/* Dual App Cards Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          
          {/* Card 1: KrPlay Receiver */}
          <div className="glass-panel rounded-2xl p-7 border-t-4 border-t-emerald-500 shadow-xl space-y-5">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-12 h-12 rounded-xl bg-emerald-500/20 p-2 flex items-center justify-center border border-emerald-500/30">
                  <img src="./krplay.svg" alt="KrPlay" className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-slate-100 flex items-center gap-2">
                    {t.receiver.title}
                  </h3>
                  <div className="text-xs text-emerald-400 font-medium">
                    {t.receiver.badge}
                  </div>
                </div>
              </div>
              <span className="text-xs font-mono px-2.5 py-1 rounded bg-slate-800 text-slate-300 border border-slate-700">
                {t.receiver.requirements}
              </span>
            </div>

            <div className="text-xs font-semibold text-slate-400 uppercase tracking-wide">
              {t.receiver.role}
            </div>

            <ul className="space-y-3 pt-2">
              {t.receiver.specs.map((item, idx) => (
                <li key={idx} className="flex items-start gap-2.5 text-sm text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Card 2: KrPlay-Client */}
          <div className="glass-panel rounded-2xl p-7 border-t-4 border-t-sky-500 shadow-xl space-y-5">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-12 h-12 rounded-xl bg-sky-500/20 p-2 flex items-center justify-center border border-sky-500/30">
                  <img src="./krplay-client.svg" alt="KrPlay-Client" className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-slate-100 flex items-center gap-2">
                    {t.client.title}
                  </h3>
                  <div className="text-xs text-sky-400 font-medium">
                    {t.client.badge}
                  </div>
                </div>
              </div>
              <span className="text-xs font-mono px-2.5 py-1 rounded bg-slate-800 text-slate-300 border border-slate-700">
                {t.client.requirements}
              </span>
            </div>

            <div className="text-xs font-semibold text-slate-400 uppercase tracking-wide">
              {t.client.role}
            </div>

            <ul className="space-y-3 pt-2">
              {t.client.specs.map((item, idx) => (
                <li key={idx} className="flex items-start gap-2.5 text-sm text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-sky-400 flex-shrink-0 mt-0.5" />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>

        </div>

        {/* Footnote callout */}
        <div className="mt-8 flex items-start gap-3 p-4 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400">
          <Info className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
          <p className="leading-relaxed">{t.note}</p>
        </div>

      </div>
    </section>
  );
};
