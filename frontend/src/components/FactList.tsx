import { Box } from '@mui/material';

// Label / value pairs, like "Car  2019 Toyota Corolla". A <dl> is the HTML element made for this
export function FactList({ facts }: { facts: [string, string][] }) {
  return (
    <Box component="dl" sx={{ m: 0, display: 'grid', gridTemplateColumns: '76px 1fr', columnGap: 1.5, rowGap: 0.75, fontSize: 16 }}>
      {facts.map(([label, value]) => (
        <Box key={label} sx={{ display: 'contents' }}>
          <Box component="dt" sx={{ color: 'text.secondary' }}>{label}</Box>
          <Box component="dd" sx={{ m: 0 }}>{value}</Box>
        </Box>
      ))}
    </Box>
  );
}