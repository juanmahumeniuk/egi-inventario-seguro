import React from 'react';

interface ItuLogoProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
}

export default function ItuLogo({ className = '', size = 'md' }: ItuLogoProps) {
  // Sizing mapping
  const sizes = {
    sm: {
      text: 'text-2xl',
      circle: 'w-2.5 h-2.5',
      virtual: 'text-[9px] tracking-[0.2em]',
      gap: 'gap-0.5',
      logoW: 'w-16',
      height: 'h-6',
    },
    md: {
      text: 'text-4xl',
      circle: 'w-3.5 h-3.5',
      virtual: 'text-xs tracking-[0.25em]',
      gap: 'gap-1',
      logoW: 'w-28',
      height: 'h-10',
    },
    lg: {
      text: 'text-5xl border-r pr-4',
      circle: 'w-4 h-4',
      virtual: 'text-sm tracking-[0.3em]',
      gap: 'gap-1.5',
      logoW: 'w-36',
      height: 'h-14',
    },
    xl: {
      text: 'text-7xl',
      circle: 'w-6 h-6',
      virtual: 'text-xl tracking-[0.35em]',
      gap: 'gap-2',
      logoW: 'w-56',
      height: 'h-20',
    },
  };

  const current = sizes[size];

  return (
    <div id="itu-logo-container" className={`inline-flex flex-col items-center select-none ${className}`}>
      {/* Upper part: itu logo text */}
      <div className="flex items-baseline font-black text-[#0d7671]">
        {/* The "i" with custom red dot floating */}
        <span className="relative inline-block leading-none mr-0.5">
          <span 
            className="absolute rounded-full bg-[#e52427] transition-all duration-300"
            style={{
              width: size === 'sm' ? '0.4rem' : size === 'md' ? '0.625rem' : size === 'lg' ? '0.75rem' : '1.1rem',
              height: size === 'sm' ? '0.4rem' : size === 'md' ? '0.625rem' : size === 'lg' ? '0.75rem' : '1.1rem',
              top: size === 'sm' ? '-0.3rem' : size === 'md' ? '-0.45rem' : size === 'lg' ? '-0.5rem' : '-0.75rem',
              left: '50%',
              transform: 'translateX(-50%)',
            }}
          />
          i
        </span>
        <span className="leading-none">t</span>
        <span className="leading-none">u</span>

        {/* Dynamic separator bar for side layouts if size is lg/xl */}
        {size === 'lg' && (
          <div className="h-10 w-[2px] bg-slate-200 mx-4 self-center" />
        )}
      </div>

      {/* Red divider line & VIRTUAL label */}
      <div className="w-full flex flex-col items-center mt-0.5">
        <div className="w-full h-[2px] bg-[#e52427]" />
        <span className={`font-semibold text-slate-500 uppercase ${current.virtual} text-center mt-1`}>
          Virtual
        </span>
      </div>
    </div>
  );
}
