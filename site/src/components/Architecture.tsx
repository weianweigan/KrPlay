import React from 'react';
import { Info, CheckCircle2, Zap, Smartphone } from 'lucide-react';
import { Language } from '../types';
import { translations } from '../i18n/translations';

interface ArchitectureProps {
  lang: Language;
}

export const Architecture: React.FC<ArchitectureProps> = ({ lang }) => {
  const t = translations[lang].architecture;

  return (
    <section id="architecture" className="py-20 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3 mb-14">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full zeekr-pill text-xs font-semibold text-[#3B5BDB] dark:text-[#5C7CFA] border border-[#3B5BDB]/25">
            <Zap className="w-3.5 h-3.5 text-[#3B5BDB]" />
            <span>{t.badge}</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-[var(--text-primary)]">
            {t.title}
          </h2>
          <p className="text-sm sm:text-base text-[var(--text-secondary)] max-w-2xl mx-auto">
            {t.subtitle}
          </p>
        </div>

        {/* Network Topology Flow Visual - Zeekr Bento Style (Design.md 4 & 5) */}
        <div className="mb-14 p-6 sm:p-8 rounded-[24px] zeekr-card border border-white/60 dark:border-white/10 shadow-xl">
          <div className="text-xs font-mono font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-8 flex items-center justify-between">
            <span className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-[#3B5BDB]" />
              两级网络拓扑架构 (Connection Topology)
            </span>
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-[#3B5BDB]/10 text-[#3B5BDB] dark:bg-[#3B5BDB]/20 dark:text-[#5C7CFA]">
              计算隔离 · 车机零负荷
            </span>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-stretch relative">
            {/* Stage 1: iPhone */}
            <div className="flex flex-col items-center text-center p-6 rounded-[20px] zeekr-pill border border-white/40 dark:border-white/10 space-y-3 relative group">
              <div className="w-14 h-14 rounded-[16px] bg-gradient-to-br from-black/5 to-black/10 dark:from-white/10 dark:to-white/5 flex items-center justify-center text-2xl shadow-inner border border-white/30 dark:border-white/10 group-hover:scale-105 transition-transform">
                <Smartphone className="w-7 h-7 text-[var(--text-primary)]" />
              </div>
              <div className="font-bold text-base text-[var(--text-primary)]">iPhone</div>
              <div className="text-xs text-[var(--text-secondary)] leading-relaxed">
                运行原生 iOS CarPlay 系统服务与音乐/导航
              </div>
              <div className="pt-2">
                <span className="text-[11px] font-mono text-[#2B8A3E] dark:text-[#40C057] bg-[#2B8A3E]/10 dark:bg-[#2B8A3E]/20 px-3 py-1 rounded-full border border-[#2B8A3E]/20 font-semibold">
                  热点 5GHz / USB 直连
                </span>
              </div>
            </div>

            {/* Stage 2: Receiver Phone (KrPlay) */}
            <div className="flex flex-col items-center text-center p-6 rounded-[20px] zeekr-card !bg-white/80 dark:!bg-[#181D2A]/85 border-2 border-[#2B8A3E]/30 dark:border-[#40C057]/30 space-y-3 relative group shadow-lg">
              <div className="absolute -top-3 px-3 py-0.5 rounded-full bg-[#2B8A3E] text-white text-[10px] font-black uppercase tracking-wider shadow-sm">
                协议解析与编解码枢纽
              </div>
              <div className="w-14 h-14 rounded-[16px] bg-[#2B8A3E]/15 flex items-center justify-center p-3 border border-[#2B8A3E]/30 group-hover:scale-105 transition-transform">
                <img src="./krplay.svg" alt="KrPlay" className="w-full h-full object-contain" />
              </div>
              <div className="font-bold text-base text-[#2B8A3E] dark:text-[#40C057]">
                闲置安卓设备 (KrPlay)
              </div>
              <div className="text-xs text-[var(--text-secondary)] leading-relaxed">
                接收苹果认证握手 · 视频硬解压包 · 借网 VPN 路由
              </div>
              <div className="pt-2">
                <span className="text-[11px] font-mono text-[#3B5BDB] dark:text-[#5C7CFA] bg-[#3B5BDB]/10 dark:bg-[#3B5BDB]/20 px-3 py-1 rounded-full border border-[#3B5BDB]/20 font-semibold">
                  局域网超低延迟 TCP 推流
                </span>
              </div>
            </div>

            {/* Stage 3: Car Head Unit (KrPlay-Client) */}
            <div className="flex flex-col items-center text-center p-6 rounded-[20px] zeekr-card !bg-white/80 dark:!bg-[#181D2A]/85 border-2 border-[#3B5BDB]/30 dark:border-[#5C7CFA]/30 space-y-3 relative group shadow-lg">
              <div className="absolute -top-3 px-3 py-0.5 rounded-full bg-[#3B5BDB] text-white text-[10px] font-black uppercase tracking-wider shadow-sm">
                极氪 8295 专属客户端
              </div>
              <div className="w-14 h-14 rounded-[16px] bg-[#3B5BDB]/15 flex items-center justify-center p-3 border border-[#3B5BDB]/30 group-hover:scale-105 transition-transform">
                <img src="./krplay-client.svg" alt="KrPlay-Client" className="w-full h-full object-contain" />
              </div>
              <div className="font-bold text-base text-[#3B5BDB] dark:text-[#5C7CFA]">
                车机大屏 (KrPlay-Client)
              </div>
              <div className="text-xs text-[var(--text-secondary)] leading-relaxed">
                8295 GPU 流畅硬件渲染 · 多点触控映射 · 车机麦克风输入
              </div>
              <div className="pt-2">
                <span className="text-[11px] font-mono text-[var(--text-primary)] bg-black/5 dark:bg-white/10 px-3 py-1 rounded-full border border-black/5 dark:border-white/10 font-semibold">
                  全屏沉浸驾驶交互
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Dual App Cards Grid - Zeekr Bento Style */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          
          {/* Card 1: KrPlay Receiver */}
          <div className="zeekr-card p-7 !rounded-[24px] border-t-4 border-t-[#2B8A3E] space-y-6 zeekr-card-hover">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-12 h-12 rounded-[14px] bg-[#2B8A3E]/15 p-2 flex items-center justify-center border border-[#2B8A3E]/30">
                  <img src="./krplay.svg" alt="KrPlay" className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-[var(--text-primary)]">
                    {t.receiver.title}
                  </h3>
                  <div className="text-xs text-[#2B8A3E] dark:text-[#40C057] font-semibold mt-0.5">
                    {t.receiver.badge}
                  </div>
                </div>
              </div>
              <span className="text-xs font-mono px-3 py-1 rounded-full zeekr-pill font-medium text-[var(--text-secondary)]">
                {t.receiver.requirements}
              </span>
            </div>

            <div className="text-xs font-semibold text-[var(--text-muted)] uppercase tracking-wider border-b border-black/5 dark:border-white/5 pb-2">
              {t.receiver.role}
            </div>

            <ul className="space-y-3 pt-1">
              {t.receiver.specs.map((item, idx) => (
                <li key={idx} className="flex items-start gap-2.5 text-sm text-[var(--text-secondary)]">
                  <CheckCircle2 className="w-4 h-4 text-[#2B8A3E] dark:text-[#40C057] flex-shrink-0 mt-0.5" />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Card 2: KrPlay-Client */}
          <div className="zeekr-card p-7 !rounded-[24px] border-t-4 border-t-[#3B5BDB] space-y-6 zeekr-card-hover">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-12 h-12 rounded-[14px] bg-[#3B5BDB]/15 p-2 flex items-center justify-center border border-[#3B5BDB]/30">
                  <img src="./krplay-client.svg" alt="KrPlay-Client" className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-[var(--text-primary)]">
                    {t.client.title}
                  </h3>
                  <div className="text-xs text-[#3B5BDB] dark:text-[#5C7CFA] font-semibold mt-0.5">
                    {t.client.badge}
                  </div>
                </div>
              </div>
              <span className="text-xs font-mono px-3 py-1 rounded-full zeekr-pill font-medium text-[var(--text-secondary)]">
                {t.client.requirements}
              </span>
            </div>

            <div className="text-xs font-semibold text-[var(--text-muted)] uppercase tracking-wider border-b border-black/5 dark:border-white/5 pb-2">
              {t.client.role}
            </div>

            <ul className="space-y-3 pt-1">
              {t.client.specs.map((item, idx) => (
                <li key={idx} className="flex items-start gap-2.5 text-sm text-[var(--text-secondary)]">
                  <CheckCircle2 className="w-4 h-4 text-[#3B5BDB] dark:text-[#5C7CFA] flex-shrink-0 mt-0.5" />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>

        </div>

        {/* Footnote callout - Zeekr Pill Box */}
        <div className="mt-8 flex items-start gap-3 p-4 rounded-[18px] zeekr-pill text-xs text-[var(--text-secondary)]">
          <Info className="w-4 h-4 text-[#3B5BDB] dark:text-[#5C7CFA] flex-shrink-0 mt-0.5" />
          <p className="leading-relaxed">{t.note}</p>
        </div>

      </div>
    </section>
  );
};

