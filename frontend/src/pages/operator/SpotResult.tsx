import { Box, Stack, Typography } from '@mui/material';
import type { SpotLookup } from '../../api/lookup';
import { formatDate, formatTime } from '../../format';
import { fonts } from '../../theme/theme';
import { plateStyle } from './PlateResult';

// A spot answer isn't a tow verdict on its own, so it uses a neutral panel instead of a colored band
export function SpotResult({ result }: { result: SpotLookup }) {
  const vehicles = result.vehicles ?? [];

  return (
    <Stack spacing={1.75} sx={{ mt: 2.25 }}>
      <Box role="status" sx={{ bgcolor: 'background.paper', borderRadius: 1, p: 2 }}>
        <Typography component="h2" sx={{ fontFamily: fonts.display, fontWeight: 700, fontSize: 32, lineHeight: 1 }}>
          Spot {result.spotLabel}
        </Typography>
        <Typography sx={{ mt: 1, fontSize: 17 }}>
          {result.assigned
            ? `Belongs to unit ${result.unitNumber}, ${result.validUntil ? `until ${formatDate(result.validUntil)}` : 'no expiry'}.`
            : "Isn't assigned to anyone."}
        </Typography>
      </Box>

      {result.assigned && (
        <>
          <Typography variant="h2">Cars allowed here</Typography>
          {vehicles.length === 0 && (
            <Typography color="text.secondary">Unit {result.unitNumber} has no cars registered.</Typography>
          )}
          {vehicles.map((vehicle) => (
            <Box key={vehicle.plate} sx={{ bgcolor: 'background.paper', borderRadius: 1, px: 1.5, py: 1.25 }}>
              <Typography sx={plateStyle}>{vehicle.plate}</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {vehicle.year} {vehicle.make} {vehicle.model}, {vehicle.color?.toLowerCase()}
              </Typography>
            </Box>
          ))}
          <Typography variant="body2" color="text.secondary">
            If the car parked here isn't one of these, check its plate.
          </Typography>
        </>
      )}

      {result.checkedAt && (
        <Typography variant="body2" color="text.secondary">Checked at {formatTime(result.checkedAt)}</Typography>
      )}
    </Stack>
  );
}