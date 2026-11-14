import { tokens } from '../theme/theme';

// The three verdict icons. Each has its own shape, so the meaning survives glare and color blindness
type IconProps = { size?: number };

export function CheckIcon({ size = 34 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="11" fill="#FFFFFF" />
      <path d="M7 12.5l3.2 3.2L17.5 8.5" fill="none" stroke={tokens.ok} strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function WarningIcon({ size = 34, color = tokens.onHazard, markColor = tokens.hazard }: IconProps & { color?: string; markColor?: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 2L23 21H1z" fill={color} />
      <path d="M12 9v5" stroke={markColor} strokeWidth="2.4" strokeLinecap="round" />
      <circle cx="12" cy="17.4" r="1.4" fill={markColor} />
    </svg>
  );
}

export function StopIcon({ size = 34 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M7.2 1h9.6L23 7.2v9.6L16.8 23H7.2L1 16.8V7.2z" fill="#FFFFFF" />
      <path d="M8.5 8.5l7 7M15.5 8.5l-7 7" stroke={tokens.stop} strokeWidth="2.8" strokeLinecap="round" />
    </svg>
  );
}

export function ChevronDownIcon({ size = 18 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}