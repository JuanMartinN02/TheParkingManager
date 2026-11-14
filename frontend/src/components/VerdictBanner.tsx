import { Box, Typography } from '@mui/material';
import { fonts, tokens } from '../theme/theme';
import { CheckIcon, StopIcon, WarningIcon } from './icons';

export type Verdict = 'authorized' | 'noPermit' | 'notRegistered';

const looks = {
  authorized: { background: tokens.ok, color: '#FFFFFF', title: 'Authorized', icon: <CheckIcon /> },
  noPermit: { background: tokens.hazard, color: tokens.onHazard, title: 'No active permit', icon: <WarningIcon /> },
  notRegistered: { background: tokens.stop, color: '#FFFFFF', title: 'Not registered', icon: <StopIcon /> },
};

// Hazard tape exists only here, only for the case where towing could be a mistake
const hazardTape = `repeating-linear-gradient(-45deg, ${tokens.onHazard} 0 12px, ${tokens.hazard} 12px 24px)`;

export function VerdictBanner({ verdict, message }: { verdict: Verdict; message: string }) {
  const look = looks[verdict];
  const taped = verdict === 'noPermit';

  return (
    // role="status" makes screen readers announce each new verdict
    <Box role="status" sx={{
      position: 'relative',
      bgcolor: look.background,
      color: look.color,
      px: 2,
      py: taped ? 3.75 : 2.5,
      ...(taped && {
        '&::before, &::after': { content: '""', position: 'absolute', left: 0, right: 0, height: 12, background: hazardTape },
        '&::before': { top: 0 },
        '&::after': { bottom: 0 },
      }),
    }}>
      <Typography component="h2" sx={{ display: 'flex', alignItems: 'center', gap: 1.25, fontFamily: fonts.display, fontWeight: 700, fontSize: 38, lineHeight: 1 }}>
        {look.icon}
        {look.title}
      </Typography>
      <Typography sx={{ mt: 1.25, fontSize: 17, fontWeight: 500, maxWidth: '32ch' }}>{message}</Typography>
    </Box>
  );
}