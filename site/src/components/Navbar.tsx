import React, { useState } from 'react';
import { Sun, Moon, Globe, Github, Menu, X, ArrowUpRight, BatteryCharging, Radio } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { Language } from '../types';
import { translations } from '../i18n/translations';
import { GITHUB_REPO_URL } from '../services/releaseService';

interface NavbarProps {
  lang: Language;
  setLang: (lang: Language) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ lang, setLang }) => {
  const { theme, toggleTheme } = useTheme();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const t = translations[lang].nav;

  const navLinks = [
    { name: t.features, href: '#features' },
    { name: t.architecture, href: '#architecture' },
    { name: t.guide, href: '#guide' },
    { name: t.downloads, href: '#downloads' },
    { name: t.compliance, href: '#compliance' },
  ];

  return (
    <header className="sticky top-0 z-50 px-4 sm:px-6 lg:px-8 pt-3 pb-2 transition-colors duration-200 pointer-events-none">
      <div className="max-w-7xl mx-auto pointer-events-auto">
        <div className="zeekr-card px-4 sm:px-5 py-2.5 flex items-center justify-between !rounded-full shadow-lg border border-white/40 dark:border-white/10 backdrop-blur-xl">
          
          {/* Logo & Vehicle System Badge */}
          <a href="#" className="flex items-center gap-3 group zeekr-interactive">
            <div className="relative w-9 h-9 flex items-center justify-center rounded-2xl bg-gradient-to-br from-[#3B5BDB] via-[#4C6EF5] to-[#7B52AB] p-0.5 shadow-md shadow-blue-500/20 group-hover:scale-105 transition-transform">
              <div className="w-full h-full bg-[#14151A] rounded-[14px] flex items-center justify-center overflow-hidden">
                <img src="./krplay.svg" alt="KrPlay" className="w-5 h-5 object-contain" />
              </div>
            </div>
            <div className="flex flex-col">
              <div className="flex items-center gap-2">
                <span className="font-bold text-base tracking-tight text-[var(--text-primary)]">
                  KrPlay
                </span>
                <span className="hidden sm:inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-[#3B5BDB]/10 text-[#3B5BDB] dark:bg-[#3B5BDB]/20 dark:text-[#5C7CFA] border border-[#3B5BDB]/20">
                  <Radio className="w-2.5 h-2.5 animate-pulse text-[#3B5BDB]" />
                  Zeekr OS 7.2.3
                </span>
              </div>
              <span className="text-[10px] text-[var(--text-muted)] tracking-wider font-mono">
                8295 COCKPIT SUITE
              </span>
            </div>
          </a>

          {/* Desktop Navigation Links (Pill Style) */}
          <nav className="hidden md:flex items-center gap-1 bg-black/5 dark:bg-white/5 p-1 rounded-full border border-black/5 dark:border-white/5">
            {navLinks.map(link => (
              <a
                key={link.href}
                href={link.href}
                className="px-3.5 py-1.5 rounded-full text-xs font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-white/70 dark:hover:bg-white/10 transition-all"
              >
                {link.name}
              </a>
            ))}
          </nav>

          {/* Cockpit Status Capsule & Controls */}
          <div className="hidden sm:flex items-center gap-2.5">
            {/* Battery / Performance Status Pill (Design.md 4.0) */}
            <div className="hidden lg:flex items-center gap-2 px-3 py-1.5 rounded-full zeekr-pill text-[11px] font-medium text-[var(--text-secondary)]">
              <BatteryCharging className="w-3.5 h-3.5 text-[#2B8A3E] dark:text-[#40C057]" />
              <span className="font-mono font-semibold text-[var(--text-primary)]">324km</span>
              <span className="opacity-40">|</span>
              <span className="font-mono text-[#2B8A3E] dark:text-[#40C057] font-semibold">66%</span>
            </div>

            {/* Language Switcher Pill */}
            <button
              onClick={() => setLang(lang === 'zh' ? 'en' : 'zh')}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full zeekr-pill text-xs font-semibold text-[var(--text-primary)] hover:bg-black/5 dark:hover:bg-white/10 transition-all zeekr-interactive"
              title="切换语言 / Switch Language"
            >
              <Globe className="w-3.5 h-3.5 text-[#3B5BDB]" />
              <span>{lang === 'zh' ? 'EN' : '中文'}</span>
            </button>

            {/* Theme Toggle Pill */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-full zeekr-pill text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-black/5 dark:hover:bg-white/10 transition-all zeekr-interactive"
              title={theme === 'dark' ? '切换浅色模式' : '切换深色模式'}
            >
              {theme === 'dark' ? (
                <Sun className="w-4 h-4 text-amber-400" />
              ) : (
                <Moon className="w-4 h-4 text-[#3B5BDB]" />
              )}
            </button>

            {/* GitHub CTA Button (Zeekr Dark CTA Button - Design.md 2.3) */}
            <a
              href={GITHUB_REPO_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-semibold bg-[#141517] dark:bg-white text-white dark:text-[#141517] hover:opacity-90 shadow-sm transition-all zeekr-interactive"
            >
              <Github className="w-3.5 h-3.5" />
              <span>{t.github}</span>
              <ArrowUpRight className="w-3 h-3 opacity-70" />
            </a>
          </div>

          {/* Mobile Menu Button */}
          <div className="flex sm:hidden items-center gap-2">
            <button
              onClick={() => setLang(lang === 'zh' ? 'en' : 'zh')}
              className="px-2.5 py-1 rounded-full zeekr-pill text-xs font-semibold text-[var(--text-primary)]"
            >
              {lang === 'zh' ? 'EN' : '中'}
            </button>
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-full zeekr-pill text-[var(--text-primary)]"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden mt-2 pointer-events-auto max-w-7xl mx-auto">
          <div className="zeekr-card p-4 space-y-3 !rounded-2xl border border-white/30 dark:border-white/10">
            <nav className="flex flex-col space-y-1">
              {navLinks.map(link => (
                <a
                  key={link.href}
                  href={link.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className="px-3.5 py-2.5 rounded-xl text-sm font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
                >
                  {link.name}
                </a>
              ))}
            </nav>
            <div className="pt-2 border-t border-black/5 dark:border-white/10 flex items-center justify-between">
              <button
                onClick={toggleTheme}
                className="flex items-center gap-2 text-xs font-medium text-[var(--text-primary)] py-2 px-3 rounded-full zeekr-pill"
              >
                {theme === 'dark' ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-[#3B5BDB]" />}
                <span>{theme === 'dark' ? '浅色模式' : '深色模式'}</span>
              </button>
              <a
                href={GITHUB_REPO_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1.5 text-xs font-bold text-white bg-[#141517] dark:bg-white dark:text-[#141517] py-2 px-4 rounded-full"
              >
                <Github className="w-4 h-4" />
                <span>GitHub</span>
              </a>
            </div>
          </div>
        </div>
      )}
    </header>
  );
};

