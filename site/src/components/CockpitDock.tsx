import React, { useState } from 'react';
import { 
  Home, 
  ChevronLeft, 
  ChevronRight, 
  Music, 
  Compass, 
  Sliders 
} from 'lucide-react';
import { 
  ZeekrCarFrontIcon, 
  Zeekr360CameraIcon 
} from './icons/CockpitIcons';

export const CockpitDock: React.FC = () => {
  const [acTemp, setAcTemp] = useState(22.5);
  const [acAuto, setAcAuto] = useState(true);

  const increaseTemp = () => setAcTemp(prev => Math.min(30, +(prev + 0.5).toFixed(1)));
  const decreaseTemp = () => setAcTemp(prev => Math.max(16, +(prev - 0.5).toFixed(1)));

  return (
    <aside aria-label="Cockpit Dock" className="fixed bottom-3 left-1/2 -translate-x-1/2 z-40 max-w-4xl w-[calc(100%-2rem)] pointer-events-none transition-all duration-300">
      <div className="zeekr-dock px-3 sm:px-6 py-2.5 rounded-full border border-white/50 dark:border-white/10 shadow-2xl flex items-center justify-between pointer-events-auto backdrop-blur-2xl">
        
        {/* Left: Car Control Zone (Design.md 5.4) */}
        <div className="flex items-center gap-1 sm:gap-2">
          <a
            href="#"
            className="w-10 h-10 rounded-full flex items-center justify-center text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-black/5 dark:hover:bg-white/10 zeekr-interactive"
            title="回到顶部 / 主页"
          >
            <Home className="w-5 h-5" />
          </a>
          <a
            href="#architecture"
            className="w-10 h-10 rounded-full flex items-center justify-center text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-black/5 dark:hover:bg-white/10 zeekr-interactive relative"
            title="双端架构设置"
          >
            <ZeekrCarFrontIcon size={20} />
            <span className="absolute bottom-1 w-2 h-0.5 rounded-full bg-[#F76707]" />
          </a>
          <a
            href="#features"
            className="hidden sm:flex w-10 h-10 rounded-full items-center justify-center text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-black/5 dark:hover:bg-white/10 zeekr-interactive"
            title="特性控制"
          >
            <Sliders className="w-5 h-5" />
          </a>
        </div>

        {/* Middle: Air Conditioning Temperature Controller (Design.md 5.4) */}
        <div className="flex items-center gap-1 sm:gap-2 px-3 py-1 rounded-full zeekr-pill border border-white/30 dark:border-white/5">
          <button 
            onClick={decreaseTemp}
            className="p-1 rounded-full text-[var(--text-muted)] hover:text-[var(--text-primary)] zeekr-interactive"
            title="降低温度"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          
          <div 
            onClick={() => setAcAuto(!acAuto)}
            className="flex flex-col items-center cursor-pointer select-none px-1.5"
            title="点击切换自动空调"
          >
            <div className="flex items-baseline font-mono">
              <span className="text-base sm:text-lg font-bold text-[var(--text-primary)]">{acTemp}</span>
              <span className="text-xs text-[var(--text-muted)] ml-0.5">°</span>
            </div>
            {/* Orange heating status line + AUTO tag */}
            <div className="flex items-center gap-1 -mt-0.5">
              <span className={`w-3 h-0.5 rounded-full ${acAuto ? 'bg-[#F76707]' : 'bg-gray-400'}`} />
              <span className="text-[9px] font-bold text-[var(--text-muted)] tracking-wider">AUTO</span>
            </div>
          </div>

          <button 
            onClick={increaseTemp}
            className="p-1 rounded-full text-[var(--text-muted)] hover:text-[var(--text-primary)] zeekr-interactive"
            title="升高温度"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>

        {/* Right: Quick App Icons (Design.md 5.4) */}
        <div className="flex items-center gap-1.5 sm:gap-2.5">
          {/* KrPlay Head Unit Client */}
          <a
            href="#downloads"
            className="w-10 h-10 rounded-[14px] bg-gradient-to-br from-[#3B5BDB] to-[#7B52AB] p-0.5 shadow-md flex items-center justify-center zeekr-interactive group"
            title="下载 KrPlay"
          >
            <div className="w-full h-full bg-[#14151A] rounded-[12px] flex items-center justify-center overflow-hidden">
              <img src="./krplay.svg" alt="KrPlay" className="w-5 h-5 object-contain group-hover:scale-110 transition-transform" />
            </div>
          </a>

          {/* Media shortcut */}
          <a
            href="#features"
            className="w-10 h-10 rounded-[14px] bg-gradient-to-br from-rose-500 to-red-600 flex items-center justify-center text-white shadow-sm zeekr-interactive"
            title="车载音乐流"
          >
            <Music className="w-5 h-5" />
          </a>

          {/* Navigation shortcut */}
          <a
            href="#guide"
            className="hidden sm:flex w-10 h-10 rounded-[14px] bg-gradient-to-br from-sky-500 to-blue-600 items-center justify-center text-white shadow-sm zeekr-interactive"
            title="车载快速指南"
          >
            <Compass className="w-5 h-5" />
          </a>

          {/* 360 / Camera shortcut */}
          <a
            href="#architecture"
            className="hidden md:flex w-10 h-10 rounded-[14px] bg-gradient-to-br from-emerald-500 to-teal-600 items-center justify-center text-white shadow-sm zeekr-interactive"
            title="全景监控 / 链路状态"
          >
            <Zeekr360CameraIcon size={20} />
          </a>
        </div>

      </div>
    </aside>
  );
};
