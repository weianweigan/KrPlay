import React from 'react';

// ==========================================
// Zeekr OS 7.2.3 Authentic Cockpit Vector Icons
// ==========================================

interface IconProps extends React.SVGProps<SVGSVGElement> {
  size?: number;
  className?: string;
}

/** 1. Vehicle Contour (Front View - Zeekr Style) */
export const ZeekrCarFrontIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    <path d="M5 17h14" />
    <path d="M6 10l1.8-4.5c.3-.8 1.1-1.5 2-1.5h4.4c.9 0 1.7.7 2 1.5L18 10" />
    <rect x="3" y="10" width="18" height="8" rx="2" />
    <circle cx="7" cy="14" r="1.25" fill="currentColor" stroke="none" />
    <circle cx="17" cy="14" r="1.25" fill="currentColor" stroke="none" />
    <path d="M9 14h6" strokeWidth="1.2" />
    <path d="M5 18v2a1 1 0 001 1h1a1 1 0 001-1v-2" />
    <path d="M16 18v2a1 1 0 001 1h1a1 1 0 001-1v-2" />
  </svg>
);

/** 2. Automotive Power Seat (Headrest, Backrest & Cushion) */
export const ZeekrSeatIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    {/* Headrest */}
    <rect x="7" y="3" width="5" height="3" rx="1.5" />
    {/* Backrest */}
    <path d="M8 7h3.5a1.5 1.5 0 011.5 1.5v7a1.5 1.5 0 01-1.5 1.5H7.5c-.8 0-1.5-.7-1.5-1.5V8.5C6 7.7 6.7 7 7.5 7H8z" />
    {/* Cushion */}
    <path d="M6 17h11.5c1.1 0 2 .9 2 2v.5a1.5 1.5 0 01-1.5 1.5H7.5A2.5 2.5 0 015 18.5V17" />
    {/* Base support rail */}
    <path d="M8 21v1M15 21v1" />
  </svg>
);

/** 3. Frunk / Glovebox Drawer Icon */
export const ZeekrGloveboxIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    <rect x="4" y="6" width="16" height="12" rx="2" />
    <path d="M4 11h16" strokeDasharray="1 1" />
    <path d="M10 14h4" strokeWidth="2" />
  </svg>
);

/** 4. Dual Lightning EV Charging Port */
export const ZeekrChargingIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    <path d="M8 4l-3 7h5l-2 9 7-10h-5l3-6H8z" fill="currentColor" stroke="none" opacity="0.9" />
    <path d="M17 6l-2 5h3.5l-1.5 7 5-7.5H18l2-4.5h-3z" fill="currentColor" stroke="none" opacity="0.6" />
  </svg>
);

/** 5. 360 Panoramic Camera Icon */
export const Zeekr360CameraIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    <path d="M4 12c0-4.4 3.6-8 8-8s8 3.6 8 8-3.6 8-8 8" />
    <circle cx="12" cy="12" r="3" />
    <path d="M12 4V2M12 22v-2M20 12h2M2 12h2" />
    <path d="M18 18l2 2M4 4l2 2" opacity="0.6" />
  </svg>
);

/** 6. Eva Cockpit 3D AI Assistant Avatar (Futuristic Headphones & Visor) */
export const ZeekrEvaAvatar: React.FC<{ size?: number; className?: string }> = ({ size = 32, className = '' }) => (
  <div style={{ width: size, height: size }} className={`relative rounded-full flex items-center justify-center bg-gradient-to-tr from-purple-500 via-indigo-500 to-pink-400 p-0.5 shadow-md ${className}`}>
    <div className="w-full h-full bg-[#181B26] rounded-full flex items-center justify-center overflow-hidden relative">
      {/* Anime cyber visor */}
      <div className="w-4 h-2 rounded-full bg-cyan-400 shadow-[0_0_8px_#38bdf8] animate-pulse" />
      {/* Headphone earcups */}
      <div className="absolute -left-0.5 w-1.5 h-3 rounded-r-md bg-purple-400" />
      <div className="absolute -right-0.5 w-1.5 h-3 rounded-l-md bg-purple-400" />
      {/* Headband arc */}
      <div className="absolute top-0.5 w-5 h-2 border-t-2 border-purple-400 rounded-t-full" />
    </div>
  </div>
);

/** 7. Jay Chou Nocturne / Waiting for Class Album Artwork Card */
export const JayChouAlbumArt: React.FC<{ size?: number; className?: string }> = ({ size = 48, className = '' }) => (
  <div 
    style={{ width: size, height: size }} 
    className={`relative rounded-[12px] bg-gradient-to-br from-[#1A1A28] via-[#2B2338] to-[#121B2A] flex-shrink-0 overflow-hidden shadow-md border border-white/20 flex flex-col justify-between p-1.5 ${className}`}
  >
    {/* Vinyl Record Texture Overlay */}
    <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_30%,rgba(255,255,255,0.05)_31%,transparent_33%,rgba(255,255,255,0.05)_50%,transparent_52%)] pointer-events-none" />
    <div className="flex justify-between items-start z-10">
      <span className="text-[7px] font-mono font-bold text-amber-300 tracking-tighter uppercase opacity-90">JAY</span>
      <span className="text-[6px] font-mono text-purple-200 opacity-60">FLAC</span>
    </div>
    <div className="z-10">
      <div className="text-[8px] font-bold text-white tracking-tight leading-none">等你下课</div>
      <div className="text-[6px] text-white/70 tracking-tighter mt-0.5">周杰伦</div>
    </div>
  </div>
);

/** 8. Scenario Tile: 健康减压舱 3D Micro Illustration */
export const ScenarioRelaxGraphic: React.FC<{ size?: number; className?: string }> = ({ size = 28, className = '' }) => (
  <svg width={size} height={size} viewBox="0 0 32 32" fill="none" className={className}>
    <rect width="32" height="32" rx="8" fill="url(#relaxGlow)" opacity="0.4" />
    <path d="M8 20c2-5 7-7 12-7 3 0 5 1 5 1s-1 3-3 5c-3 3-9 4-14 1z" fill="#FFFFFF" fillOpacity="0.9" />
    <path d="M12 14c-1-3 1-6 4-7-2 0-4 1-5 3-1 2 0 4 1 4z" fill="#E9D5FF" />
    <circle cx="21" cy="11" r="1.5" fill="#FDE047" />
    <defs>
      <linearGradient id="relaxGlow" x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
        <stop stopColor="#A855F7" />
        <stop offset="1" stopColor="#EC4899" />
      </linearGradient>
    </defs>
  </svg>
);

/** 9. Scenario Tile: 洗车模式 3D Micro Illustration */
export const ScenarioWashGraphic: React.FC<{ size?: number; className?: string }> = ({ size = 28, className = '' }) => (
  <svg width={size} height={size} viewBox="0 0 32 32" fill="none" className={className}>
    {/* Car Silhouette with Water Drops */}
    <path d="M7 21h18c1 0 1.5-.5 1.5-1.5l-1-4c-.3-.8-1-1.5-2-1.5h-11c-1 0-1.7.7-2 1.5l-1 4c0 1 .5 1.5 1.5 1.5z" fill="#FFFFFF" fillOpacity="0.9" />
    <circle cx="11" cy="20" r="1.5" fill="#1E293B" />
    <circle cx="21" cy="20" r="1.5" fill="#1E293B" />
    {/* Water Spray Arc */}
    <path d="M16 6v3M11 8l1.5 2M21 8l-1.5 2" stroke="#67E8F9" strokeWidth="2" strokeLinecap="round" />
  </svg>
);

/** 10. NZP Metric 1: 直行通过路口箭头 (Straight Highway Lane Arrow) */
export const NzpStraightArrow: React.FC<IconProps> = ({ size = 22, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className} {...props}>
    <path d="M12 21V5M12 5l-4.5 4.5M12 5l4.5 4.5" stroke="#3B5BDB" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M7 21h10" stroke="#3B5BDB" strokeWidth="1.5" strokeLinecap="round" opacity="0.4" />
  </svg>
);

/** 11. NZP Metric 2: 成功变道平滑弧线箭头 (Curved Transition Arrow) */
export const NzpLaneChangeArrow: React.FC<IconProps> = ({ size = 22, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className} {...props}>
    <path d="M7 20c0-6 4-9 9-11" stroke="#3B5BDB" strokeWidth="2.5" strokeLinecap="round" />
    <path d="M12 6.5h4.5v4.5" stroke="#3B5BDB" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

/** 12. NZP Metric 3: 应对加塞平滑箭头 + 矢量小红车 (Cut-In Response Arrow with Mini Car) */
export const NzpCutInCarArrow: React.FC<IconProps> = ({ size = 26, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 28 24" fill="none" className={className} {...props}>
    {/* Arrow */}
    <path d="M8 20V5M8 5L4 9M8 5l4 4" stroke="#3B5BDB" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    {/* Realistic Mini Red Car (Photo Detail!) */}
    <g transform="translate(14, 8)">
      <rect x="0" y="2" width="11" height="6.5" rx="1.8" fill="#E03131" />
      <path d="M2 2l1.5-2.2c.2-.3.6-.5 1-.5h3c.4 0 .8.2 1 .5L10 2H2z" fill="#C92A2A" />
      <rect x="1.5" y="0.5" width="8" height="1.5" rx="0.5" fill="#74C0FC" opacity="0.8" />
      <circle cx="2.5" cy="8.5" r="1.1" fill="#1C1E24" />
      <circle cx="8.5" cy="8.5" r="1.1" fill="#1C1E24" />
      <rect x="9.5" y="3.5" width="1" height="1" rx="0.5" fill="#FFE066" />
    </g>
  </svg>
);

/** 13. NZP Metric 4: 汇入汇出弯道箭头 (Highway Merge Arrow) */
export const NzpMergeArrow: React.FC<IconProps> = ({ size = 22, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className} {...props}>
    <path d="M6 19c0-6 4-8 10-10M16 19V9" stroke="#3B5BDB" strokeWidth="2.2" strokeLinecap="round" />
    <path d="M13 6l3 3 3-3" stroke="#3B5BDB" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

/** 14. Bottom Dock: Apple Music App Icon */
export const AppleMusicAppIcon: React.FC<{ size?: number; className?: string }> = ({ size = 28, className = '' }) => (
  <div style={{ width: size, height: size }} className={`rounded-[8px] bg-gradient-to-b from-[#FA2D55] to-[#E61E47] flex items-center justify-center text-white shadow-sm ${className}`}>
    <svg width={size * 0.6} height={size * 0.6} viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z" />
    </svg>
  </div>
);

/** 15. Bottom Dock: VIP Crown App Icon */
export const VipCrownAppIcon: React.FC<{ size?: number; className?: string }> = ({ size = 28, className = '' }) => (
  <div style={{ width: size, height: size }} className={`rounded-[8px] bg-gradient-to-b from-[#845EC2] to-[#6C44B0] flex items-center justify-center text-amber-300 shadow-sm ${className}`}>
    <svg width={size * 0.6} height={size * 0.6} viewBox="0 0 24 24" fill="currentColor">
      <path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5zm14 3c0 .6-.4 1-1 1H6c-.6 0-1-.4-1-1v-1h14v1z" />
    </svg>
  </div>
);

/** 16. Bottom Dock: Yellow Radio/Music App Icon */
export const YellowRadioAppIcon: React.FC<{ size?: number; className?: string }> = ({ size = 28, className = '' }) => (
  <div style={{ width: size, height: size }} className={`rounded-[8px] bg-gradient-to-b from-[#FFC72C] to-[#E5AC1A] flex items-center justify-center text-[#1A1A24] shadow-sm ${className}`}>
    <svg width={size * 0.6} height={size * 0.6} viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14.5v-9l6 4.5-6 4.5z" />
    </svg>
  </div>
);

/** 17. Bottom Dock: App Launcher 2x2 Grid Icon */
export const AppLauncherGridIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className} {...props}>
    <rect x="4" y="4" width="6.5" height="6.5" rx="2" />
    <rect x="13.5" y="4" width="6.5" height="6.5" rx="2" />
    <rect x="4" y="13.5" width="6.5" height="6.5" rx="2" />
    <rect x="13.5" y="13.5" width="6.5" height="6.5" rx="2" />
  </svg>
);

/** 18. Steering Wheel Icon */
export const ZeekrSteeringWheelIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    <circle cx="12" cy="12" r="9" />
    <circle cx="12" cy="12" r="2.5" />
    <path d="M12 14.5V21" />
    <path d="M4.5 9.5L9.5 11" />
    <path d="M19.5 9.5L14.5 11" />
  </svg>
);

/** 19. Electric Trunk / Tailgate Icon */
export const ZeekrTrunkIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    <path d="M3 17h18" />
    <path d="M5 17l1.5-6h7l4 6" />
    <path d="M13.5 11l4.5-6.5" />
    <circle cx="7" cy="17" r="1.5" />
    <circle cx="17" cy="17" r="1.5" />
  </svg>
);

/** 20. Automotive Smart Lock Icon */
export const ZeekrDoorLockIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    <rect x="5" y="10" width="14" height="11" rx="3" />
    <path d="M8 10V6.5a4 4 0 018 0V10" />
    <circle cx="12" cy="15.5" r="1.5" fill="currentColor" stroke="none" />
  </svg>
);

/** 21. Air Vent / AC Wind Blow Icon */
export const ZeekrAirVentIcon: React.FC<IconProps> = ({ size = 20, className = '', ...props }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" className={className} {...props}>
    <path d="M4 8h11a3 3 0 010 6H12" />
    <path d="M4 12h14a2.5 2.5 0 010 5H15" />
    <path d="M4 16h8" />
  </svg>
);

