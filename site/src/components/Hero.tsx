import React, { useState } from 'react';
import { 
  Download, 
  Github,
  Play, 
  Pause, 
  SkipForward, 
  SkipBack, 
  Navigation, 
  Lock, 
  Grid, 
  X,
  BatteryCharging,
  Compass,
  Home,
  MapPin,
  Clock,
  Camera
} from 'lucide-react';
import { 
  ZeekrCarFrontIcon,
  ZeekrSeatIcon,
  ZeekrGloveboxIcon,
  ZeekrChargingIcon,
  Zeekr360CameraIcon,
  ZeekrEvaAvatar,
  JayChouAlbumArt,
  ScenarioRelaxGraphic,
  ScenarioWashGraphic,
  NzpStraightArrow,
  NzpLaneChangeArrow,
  NzpCutInCarArrow,
  NzpMergeArrow,
  AppleMusicAppIcon,
  VipCrownAppIcon,
  YellowRadioAppIcon,
  AppLauncherGridIcon
} from './icons/CockpitIcons';
import { Language, ReleaseInfo } from '../types';
import { translations } from '../i18n/translations';
import { GITHUB_REPO_URL } from '../services/releaseService';

interface HeroProps {
  lang: Language;
  release: ReleaseInfo;
}

export const Hero: React.FC<HeroProps> = ({ lang, release }) => {
  const t = translations[lang].hero;
  const [isPlaying, setIsPlaying] = useState(true);
  const [activeSideTab, setActiveSideTab] = useState<'car' | 'nav' | 'ai' | 'lock' | 'box' | 'charge' | 'menu'>('car');

  return (
    <section className="relative overflow-hidden pt-6 pb-20 md:pt-10 md:pb-28">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Top Header - Concise, Clean & Breathable */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-10">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full zeekr-pill border border-black/5 dark:border-white/10 text-xs font-semibold shadow-sm">
            <span className="w-2 h-2 rounded-full bg-[#3B5BDB]" />
            <span className="text-[var(--text-primary)]">Zeekr OS 7.2.3 智能座舱互联</span>
            <span className="text-[var(--text-muted)]">|</span>
            <span className="font-mono text-[var(--text-secondary)]">{release.tag}</span>
          </div>

          {/* Main Title */}
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight text-[var(--text-primary)] leading-tight">
            <span>{t.tagline}</span>
            <span className="block mt-1 text-[#3B5BDB] dark:text-[#5C7CFA]">
              {t.highlightTitle}
            </span>
          </h1>

          {/* Subtitle */}
          <p className="text-sm sm:text-base text-[var(--text-secondary)] max-w-2xl mx-auto leading-relaxed">
            {t.description}
          </p>

          {/* CTA Actions */}
          <div className="pt-2 flex flex-wrap items-center justify-center gap-3">
            <a
              href={release.assets.receiverApk?.downloadUrl || '#downloads'}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-[12px] bg-[#141517] dark:bg-white text-white dark:text-[#141517] text-xs font-bold shadow-md zeekr-interactive"
            >
              <Download className="w-3.5 h-3.5" />
              <span>{t.downloadReceiver}</span>
            </a>
            <a
              href={release.assets.clientApk?.downloadUrl || '#downloads'}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-[12px] bg-[#3B5BDB] hover:bg-[#4C6EF5] text-white text-xs font-bold shadow-md shadow-blue-500/20 zeekr-interactive"
            >
              <Download className="w-3.5 h-3.5" />
              <span>{t.downloadClient}</span>
            </a>
            <a
              href={GITHUB_REPO_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-[12px] zeekr-pill border border-black/5 dark:border-white/10 text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs font-semibold zeekr-interactive"
            >
              <Github className="w-3.5 h-3.5" />
              <span>{t.viewGithub}</span>
            </a>
          </div>
        </div>

        {/* ============================================================== */}
        {/* Cockpit Screen Simulator - Faithful 1:1 Recreation of User's Photo */}
        {/* ============================================================== */}
        <div className="relative max-w-5xl mx-auto">
          {/* Automotive Display Bezel */}
          <div className="relative rounded-[28px] p-3 sm:p-5 shadow-2xl border border-white/90 dark:border-white/10 bg-gradient-to-b from-[#E8EEF6] via-[#DCE3EE] to-[#CAD3E2] dark:from-[#151922] dark:via-[#11141C] dark:to-[#0B0D12] overflow-hidden select-none">
            
            {/* Top Cockpit Status Bar (Left: Avatar + Battery Capsule, Right: Eva 3D) */}
            <div className="flex items-center justify-between px-3 py-1.5 mb-3 text-xs">
              <div className="flex items-center gap-2.5">
                {/* Driver Avatar with EV Car Icon */}
                <div className="w-7 h-7 rounded-full overflow-hidden bg-white/80 dark:bg-slate-700 flex items-center justify-center text-xs shadow-sm border border-white/90 text-[#3B5BDB]">
                  <ZeekrCarFrontIcon size={15} />
                </div>
                {/* Battery Pill (Photo: [ 🔋 324km | 66% ]) */}
                <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-white/70 dark:bg-white/10 backdrop-blur-md border border-white/80 dark:border-white/10 text-[11px] font-medium text-[var(--text-primary)] shadow-sm">
                  <span className="w-2.5 h-3.5 rounded-[2px] bg-[#2B8A3E] inline-flex items-center justify-center p-0.5">
                    <BatteryCharging className="w-2 h-2 text-white" />
                  </span>
                  <span className="font-mono font-bold">324km</span>
                  <span className="opacity-30">|</span>
                  <span className="font-mono">66%</span>
                </div>
              </div>

              {/* Right: Eva Cockpit AI Assistant Avatar (Vector Icon) */}
              <div className="flex items-center gap-2">
                <ZeekrEvaAvatar size={30} />
              </div>
            </div>

            {/* Main Cockpit Body: Left Pillar + Bento Grid + Right NZP Modal */}
            <div className="flex gap-3.5 items-stretch">
              
              {/* -------------------------------------------------------- */}
              {/* 1. Left Quick-Control Pillar (Vertical Capsule in Photo) */}
              {/* -------------------------------------------------------- */}
              <div className="w-[56px] sm:w-[62px] flex-shrink-0 rounded-[30px] py-4 flex flex-col items-center justify-between bg-white/75 dark:bg-white/10 backdrop-blur-xl border border-white/90 dark:border-white/10 shadow-sm relative overflow-hidden">
                {/* Bottom Aurora Glow (as shown in photo) */}
                <div className="absolute bottom-0 left-0 right-0 h-24 bg-gradient-to-t from-indigo-500/20 via-purple-500/10 to-transparent pointer-events-none" />

                {/* Top icons */}
                <div className="flex flex-col items-center gap-4 z-10">
                  {/* Navigation Arrow */}
                  <button 
                    onClick={() => setActiveSideTab('nav')}
                    className={`w-10 h-10 rounded-[14px] flex items-center justify-center transition-all ${activeSideTab === 'nav' ? 'bg-[#3B5BDB] text-white shadow-md' : 'text-[#14151A] dark:text-white hover:bg-black/5'}`}
                    title="导航"
                  >
                    <Navigation className="w-5 h-5 fill-current" />
                  </button>

                  {/* Car Vehicle Icon in Blue Rounded Box (Photo Active State) */}
                  <button 
                    onClick={() => setActiveSideTab('car')}
                    className={`w-10 h-10 rounded-[14px] flex items-center justify-center transition-all ${activeSideTab === 'car' ? 'bg-[#3B5BDB] text-white shadow-md' : 'text-[#14151A] dark:text-white hover:bg-black/5'}`}
                    title="车控设置"
                  >
                    <ZeekrCarFrontIcon size={20} />
                  </button>

                  {/* AI+ Circle */}
                  <button 
                    onClick={() => setActiveSideTab('ai')}
                    className="w-8 h-8 rounded-full flex items-center justify-center text-[10px] font-bold bg-purple-500/10 text-purple-600 dark:text-purple-300 border border-purple-500/20"
                    title="Eva 智能助手"
                  >
                    Ai+
                  </button>

                  {/* Lock with Orange Short Indicator Line (Photo Feature) */}
                  <div className="relative flex flex-col items-center" title="闭锁状态">
                    <button 
                      onClick={() => setActiveSideTab('lock')}
                      className="w-8 h-8 flex items-center justify-center text-[#14151A] dark:text-white"
                    >
                      <Lock className="w-4 h-4" />
                    </button>
                    {/* Active Orange Line underneath */}
                    <span className="w-3.5 h-[2.5px] rounded-full bg-[#F76707] -mt-0.5" />
                  </div>

                  {/* Glovebox / Frunk */}
                  <button 
                    onClick={() => setActiveSideTab('box')}
                    className="w-8 h-8 flex items-center justify-center text-[#14151A] dark:text-white opacity-80 hover:opacity-100"
                    title="手套箱 / 前备厢"
                  >
                    <ZeekrGloveboxIcon size={18} />
                  </button>

                  {/* Charging Port Icon */}
                  <button 
                    onClick={() => setActiveSideTab('charge')}
                    className="w-8 h-8 flex items-center justify-center text-[#14151A] dark:text-white opacity-80 hover:opacity-100"
                    title="充电口开关"
                  >
                    <ZeekrChargingIcon size={18} />
                  </button>
                </div>

                {/* Bottom Custom Menu icon */}
                <div className="z-10 pt-4">
                  <button 
                    onClick={() => setActiveSideTab('menu')}
                    className="w-8 h-8 flex items-center justify-center text-[#14151A] dark:text-white opacity-70 hover:opacity-100"
                    title="快捷应用列表"
                  >
                    <Grid className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* -------------------------------------------------------- */}
              {/* 2. Middle Column: Left Bento Stack (4 Rows from Photo)   */}
              {/* -------------------------------------------------------- */}
              <div className="flex-1 space-y-3">
                
                {/* Row 1: Media Player Card (极氪极拓) */}
                <div className="zeekr-card !p-3.5 !rounded-[20px] bg-white/80 dark:bg-[#181D2A]/85 border border-white/90 dark:border-white/10 shadow-sm flex flex-col justify-between">
                  <div className="flex items-center justify-between mb-1.5">
                    <div className="flex items-center gap-1.5 text-xs font-semibold text-[var(--text-primary)]">
                      <span className="w-2 h-2 rounded-full bg-[#3B5BDB]" />
                      <span>极氪极拓</span>
                      <span className="text-[10px] text-[#E03131] font-bold -mt-1">`</span>
                    </div>
                    <span className="text-xs text-[var(--text-muted)] cursor-pointer">⌄</span>
                  </div>

                  <div className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <h4 className="text-sm font-bold text-[var(--text-primary)] truncate">
                        等你下课 (with 杨瑞代)
                      </h4>
                      <p className="text-xs text-[var(--text-secondary)] mt-0.5 truncate">
                        周杰伦
                      </p>
                    </div>
                    {/* Realistic Jay Chou Album Art Card */}
                    <JayChouAlbumArt size={48} />
                  </div>

                  {/* Thin Red Accent Progress Bar (Photo Specification) */}
                  <div className="mt-2.5">
                    <div className="w-full h-[2.5px] bg-black/10 dark:bg-white/10 rounded-full overflow-hidden">
                      <div className="h-full bg-[#E03131] rounded-full w-[65%]" />
                    </div>
                  </div>

                  {/* Playback Controls */}
                  <div className="flex items-center justify-center gap-8 mt-2 text-[#14151A] dark:text-white">
                    <button className="hover:opacity-75 zeekr-interactive" title="上一曲">
                      <SkipBack className="w-4 h-4 fill-current" />
                    </button>
                    <button 
                      onClick={() => setIsPlaying(!isPlaying)}
                      className="hover:opacity-75 zeekr-interactive"
                      title={isPlaying ? "暂停" : "播放"}
                    >
                      {isPlaying ? (
                        <Pause className="w-4 h-4 fill-current" />
                      ) : (
                        <Play className="w-4 h-4 fill-current" />
                      )}
                    </button>
                    <button className="hover:opacity-75 zeekr-interactive" title="下一曲">
                      <SkipForward className="w-4 h-4 fill-current" />
                    </button>
                  </div>
                </div>

                {/* Row 2: Weather Card (江宁区 · 25° 小雨) */}
                <div className="zeekr-card !p-3.5 !rounded-[20px] bg-white/80 dark:bg-[#181D2A]/85 border border-white/90 dark:border-white/10 shadow-sm relative overflow-hidden">
                  {/* Rain background texture */}
                  <div className="absolute inset-0 bg-gradient-to-b from-sky-500/5 to-slate-500/5 pointer-events-none" />

                  <div className="flex items-center justify-between mb-1 text-xs text-[var(--text-primary)] font-semibold">
                    <div className="flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-sky-500" />
                      <span>江宁区</span>
                    </div>
                    <span className="text-[var(--text-muted)] cursor-pointer">⌄</span>
                  </div>

                  <div className="flex items-baseline gap-2 my-1">
                    <span className="text-3xl font-bold font-mono text-[var(--text-primary)]">25°</span>
                    <span className="text-xs text-[var(--text-secondary)] font-medium">小雨 21/28°</span>
                  </div>

                  <div className="pt-2 border-t border-black/5 dark:border-white/5 flex items-center justify-between text-[11px] text-[var(--text-secondary)] font-medium">
                    <span>空气质量: 优</span>
                    <span>紫外线: 最弱</span>
                    <span>海拔: 9m</span>
                  </div>
                </div>

                {/* Row 3: Dual Clock Card (时钟 · 18:15 + 表盘) */}
                <div className="zeekr-card !p-3.5 !rounded-[20px] bg-white/80 dark:bg-[#181D2A]/85 border border-white/90 dark:border-white/10 shadow-sm flex items-center justify-between">
                  <div>
                    <div className="text-xs font-semibold text-[var(--text-primary)] mb-1 flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-slate-500" />
                      <span>时钟</span>
                    </div>
                    <div className="text-3xl font-black font-mono tracking-tight text-[var(--text-primary)]">
                      18:15
                    </div>
                    <div className="text-[11px] text-[var(--text-muted)] mt-0.5">
                      7月3日 星期五
                    </div>
                  </div>

                  {/* Analog Clock Dial matching photo with ZEEKR logo */}
                  <div className="w-16 h-16 rounded-full border-2 border-black/10 dark:border-white/20 relative flex items-center justify-center shadow-inner">
                    {/* Hour marks */}
                    <div className="absolute top-1 w-0.5 h-1.5 bg-black/60 dark:bg-white/60" />
                    <div className="absolute bottom-1 w-0.5 h-1.5 bg-black/60 dark:bg-white/60" />
                    <div className="absolute left-1 w-1.5 h-0.5 bg-black/60 dark:bg-white/60" />
                    <div className="absolute right-1 w-1.5 h-0.5 bg-black/60 dark:bg-white/60" />
                    {/* Center Brand */}
                    <span className="text-[6px] font-mono tracking-wider text-[var(--text-muted)] uppercase">ZEEKR</span>
                    {/* Hour Hand */}
                    <div className="absolute w-4 h-0.5 bg-black dark:bg-white rounded origin-left rotate-[120deg]" />
                    {/* Minute Hand */}
                    <div className="absolute w-6 h-0.5 bg-black dark:bg-white rounded origin-left rotate-[90deg]" />
                    {/* Orange Second Hand (Photo Detail) */}
                    <div className="absolute w-7 h-[1px] bg-[#F76707] rounded origin-left rotate-[210deg]" />
                    <div className="w-1 h-1 rounded-full bg-[#F76707] z-10" />
                  </div>
                </div>

                {/* Row 4: 1:1 Scenario Tiles & Navigation Pill */}
                <div className="grid grid-cols-12 gap-2.5">
                  {/* Tile 1: 健康减压舱 (Purple) */}
                  <div className="col-span-4 rounded-[18px] p-2.5 aspect-square bg-gradient-to-br from-[#7B52AB] to-[#9A74C9] text-white flex flex-col justify-between shadow-sm cursor-pointer zeekr-interactive">
                    <ScenarioRelaxGraphic size={24} />
                    <span className="text-[11px] font-bold leading-tight">健康减压舱</span>
                  </div>

                  {/* Tile 2: 洗车模式 (Car 3D) */}
                  <div className="col-span-4 rounded-[18px] p-2.5 aspect-square bg-gradient-to-br from-[#4F5D8C] to-[#7C8BB5] text-white flex flex-col justify-between shadow-sm cursor-pointer zeekr-interactive">
                    <ScenarioWashGraphic size={24} />
                    <span className="text-[11px] font-bold leading-tight">洗车模式</span>
                  </div>

                  {/* Pill: 路线管理 | 我要泊车 */}
                  <div className="col-span-4 rounded-[18px] p-2 bg-white/80 dark:bg-[#181D2A]/85 border border-white/90 dark:border-white/10 flex flex-col justify-center items-center gap-1 text-[10px] font-semibold text-[var(--text-secondary)] shadow-sm">
                    <div className="flex items-center gap-1">
                      <Compass className="w-3 h-3 text-[#3B5BDB]" />
                      <span>路线管理</span>
                    </div>
                    <div className="w-full h-[1px] bg-black/5 dark:bg-white/5" />
                    <div className="flex items-center gap-1">
                      <span>我要泊车</span>
                    </div>
                  </div>
                </div>

              </div>

              {/* -------------------------------------------------------- */}
              {/* 3. Right Column: The NZP Summary Modal (NZP已完成)       */}
              {/* -------------------------------------------------------- */}
              <div className="w-[300px] sm:w-[360px] flex-shrink-0 zeekr-card !p-5 !rounded-[24px] bg-white/85 dark:bg-[#181D2A]/90 border border-white/95 dark:border-white/15 shadow-xl flex flex-col justify-between">
                <div>
                  {/* Modal Header */}
                  <div className="flex items-center justify-between pb-3 border-b border-black/5 dark:border-white/5">
                    <div className="flex items-center gap-2">
                      <span className="text-base font-bold text-[var(--text-primary)]">
                        NZP已完成
                      </span>
                      {/* Orange Tag: 全程辅助驾驶达成 (Photo Detail) */}
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-[#F06548]/15 text-[#D94826] dark:text-[#FF7A59]">
                        全程辅助驾驶达成
                      </span>
                    </div>
                    <button className="text-[var(--text-muted)] hover:text-[var(--text-primary)]">
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Core Chart Section (左环右数) */}
                  <div className="grid grid-cols-12 gap-3 py-4 items-center">
                    {/* Left: Open Donut Chart (99% / 辅助驾驶占比) */}
                    <div className="col-span-6 flex flex-col items-center justify-center relative">
                      <div className="relative w-28 h-28 flex items-center justify-center">
                        <svg className="w-full h-full -rotate-90 transform" viewBox="0 0 100 100">
                          {/* Track */}
                          <circle
                            cx="50"
                            cy="50"
                            r="38"
                            className="stroke-black/10 dark:stroke-white/10"
                            strokeWidth="8"
                            fill="transparent"
                            strokeDasharray="210"
                            strokeDashoffset="45"
                            strokeLinecap="round"
                          />
                          {/* Royal Blue Gradient Arc */}
                          <circle
                            cx="50"
                            cy="50"
                            r="38"
                            className="stroke-[#3B5BDB]"
                            strokeWidth="8"
                            fill="transparent"
                            strokeDasharray="210"
                            strokeDashoffset="55"
                            strokeLinecap="round"
                          />
                        </svg>
                        <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                          <span className="text-3xl font-black font-mono tracking-tight text-[var(--text-primary)]">
                            99<span className="text-base font-normal ml-0.5">%</span>
                          </span>
                        </div>
                      </div>
                      <span className="text-[10px] text-[var(--text-muted)] -mt-1 font-medium">辅助驾驶占比</span>
                    </div>

                    {/* Right: Mileage & Duration (Photo Layout) */}
                    <div className="col-span-6 space-y-3 pl-2">
                      <div>
                        <div className="text-[11px] text-[var(--text-muted)] font-medium">辅助驾驶里程</div>
                        <div className="flex items-baseline mt-0.5">
                          <span className="text-xl font-black font-mono text-[var(--text-primary)]">35.2</span>
                          <span className="text-xs text-[var(--text-secondary)] ml-1">km</span>
                        </div>
                      </div>
                      <div>
                        <div className="text-[11px] text-[var(--text-muted)] font-medium">辅助驾驶时长</div>
                        <div className="flex items-baseline mt-0.5">
                          <span className="text-xl font-black font-mono text-[var(--text-primary)]">1</span>
                          <span className="text-xs text-[var(--text-secondary)] mx-0.5">h</span>
                          <span className="text-xl font-black font-mono text-[var(--text-primary)]">5</span>
                          <span className="text-xs text-[var(--text-secondary)] ml-0.5">min</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 2x2 Metric Grid with Arrows & Mini Car (Photo Exact Reproduction) */}
                  <div className="grid grid-cols-2 gap-3 pt-3 pb-4 border-t border-black/5 dark:border-white/5">
                    {/* Item 1: 通过路口 83次 (Straight Arrow) */}
                    <div className="flex items-center gap-2">
                      <NzpStraightArrow size={20} />
                      <div>
                        <div className="text-[10px] text-[var(--text-muted)]">通过路口</div>
                        <div className="text-xs font-bold text-[var(--text-primary)] font-mono">83 次</div>
                      </div>
                    </div>

                    {/* Item 2: 成功变道 54次 (Curved Arrow) */}
                    <div className="flex items-center gap-2">
                      <NzpLaneChangeArrow size={20} />
                      <div>
                        <div className="text-[10px] text-[var(--text-muted)]">成功变道</div>
                        <div className="text-xs font-bold text-[var(--text-primary)] font-mono">54 次</div>
                      </div>
                    </div>

                    {/* Item 3: 应对加塞 48次 (Curved Arrow with Mini Red Car!) */}
                    <div className="flex items-center gap-2">
                      <NzpCutInCarArrow size={24} />
                      <div>
                        <div className="text-[10px] text-[var(--text-muted)]">应对加塞</div>
                        <div className="text-xs font-bold text-[var(--text-primary)] font-mono">48 次</div>
                      </div>
                    </div>

                    {/* Item 4: 汇入汇出 5次 (Merge Arrow) */}
                    <div className="flex items-center gap-2">
                      <NzpMergeArrow size={20} />
                      <div>
                        <div className="text-[10px] text-[var(--text-muted)]">汇入汇出</div>
                        <div className="text-xs font-bold text-[var(--text-primary)] font-mono">5 次</div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Bottom Solid Black Button: 分享至手机 (Photo Detail) */}
                <div className="space-y-2 pt-2">
                  <a
                    href="#downloads"
                    className="w-full flex items-center justify-center py-3 rounded-[14px] bg-[#141517] hover:bg-black text-white text-xs font-bold shadow-md zeekr-interactive"
                  >
                    分享至手机 / 立即串流
                  </a>
                  <p className="text-[10px] text-center text-[var(--text-muted)] leading-tight">
                    分享后，可在极氪APP-消息-系统信息中查看
                  </p>
                </div>
              </div>

            </div>

            {/* -------------------------------------------------------- */}
            {/* 4. Bottom Global Dock inside Cockpit Display             */}
            {/* -------------------------------------------------------- */}
            <div className="mt-3.5 pt-2.5 border-t border-black/5 dark:border-white/5 flex items-center justify-between px-2">
              {/* Left Car Controls (Home, Car, Seat) */}
              <div className="flex items-center gap-4 text-[#14151A] dark:text-white">
                <button className="cursor-pointer hover:opacity-75" title="主页">
                  <Home size={17} />
                </button>
                <button className="cursor-pointer hover:opacity-75" title="车辆控制">
                  <ZeekrCarFrontIcon size={17} />
                </button>
                <button className="cursor-pointer hover:opacity-75" title="座椅调节">
                  <ZeekrSeatIcon size={17} />
                </button>
              </div>

              {/* Middle AC Control (< 22.5° > AUTO) */}
              <div className="flex items-center gap-2 px-3 py-0.5 rounded-full bg-white/60 dark:bg-white/10 border border-white/80">
                <span className="text-xs text-[var(--text-muted)] cursor-pointer">‹</span>
                <div className="flex flex-col items-center">
                  <span className="text-xs font-bold font-mono text-[var(--text-primary)]">22.5°</span>
                  <div className="flex items-center gap-1 -mt-0.5">
                    <span className="w-2.5 h-0.5 rounded-full bg-[#F76707]" />
                    <span className="text-[8px] font-bold text-[var(--text-muted)]">AUTO</span>
                  </div>
                </div>
                <span className="text-xs text-[var(--text-muted)] cursor-pointer">›</span>
              </div>

              {/* Right App Icons (Apple Music, Camera, 360, Crown, Music, App Grid) */}
              <div className="flex items-center gap-2">
                <AppleMusicAppIcon size={24} />
                <div className="w-6 h-6 rounded-[6px] bg-slate-900 text-white flex items-center justify-center shadow-sm" title="行车记录仪">
                  <Camera size={13} className="text-white/90" />
                </div>
                <div className="w-6 h-6 rounded-[6px] bg-blue-600 text-white flex items-center justify-center shadow-sm" title="360 全景">
                  <Zeekr360CameraIcon size={14} />
                </div>
                <VipCrownAppIcon size={24} />
                <YellowRadioAppIcon size={24} />
                <div className="w-6 h-6 rounded-[6px] bg-slate-300 dark:bg-slate-700 text-slate-800 dark:text-white flex items-center justify-center" title="全部应用">
                  <AppLauncherGridIcon size={14} />
                </div>
              </div>
            </div>

          </div>
        </div>

      </div>
    </section>
  );
};



