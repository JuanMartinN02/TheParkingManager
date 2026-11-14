import { Box, Stack, Typography } from '@mui/material';
import type { PlateLookup, VehicleSummary } from '../../api/lookup';
import { FactList } from '../../components/FactList';
import { FormError } from '../../components/FormError';
import { VerdictBanner } from '../../components/VerdictBanner';
import { formatDate, formatTime } from '../../format';
import { fonts } from '../../theme/theme';

// Placeholder wording until ESW says what drivers must do here (status.md, ESW question 19)
const NO_PERMIT_ACTION = ' ';

export const plateStyle = { fontFamily: fonts.display, fontWeight: 700, fontSize: 30, lineHeight: 1, letterSpacing: '0.05em' };

function describeCar(vehicle: VehicleSummary): string {
  return `${vehicle.year ?? ''} ${vehicle.make ?? ''} ${vehicle.model ?? ''}`.trim();
}

export function PlateResult({ result, propertyName }: { result: PlateLookup; propertyName: string }) {
  const { status, plate, unitNumber, vehicle } = result;
  const authorizations = result.authorizations ?? [];

  // Never fall back to "Not registered": a wrong default here could get a resident's car towed
  if (status !== 'RESIDENT_AUTHORIZED' && status !== 'RESIDENT_REGISTERED' && status !== 'UNKNOWN') {
    return <FormError message="The server sent an answer this app doesn't understand. Don't tow, check again." />;
  }

  const carFacts: [string, string][] = vehicle
    ? [['Car', describeCar(vehicle)], ['Color', vehicle.color ?? ''], ['Unit', unitNumber ?? '']]
    : [];

  return (
    <>
      {/* Negative margin lets the band reach both edges of the screen, past the page padding */}
      <Box sx={{ mx: -2, mt: 2.25 }}>
        {status === 'RESIDENT_AUTHORIZED' && (
          <VerdictBanner verdict="authorized" message={`Resident car from unit ${unitNumber}. Leave it.`} />
        )}
        {status === 'RESIDENT_REGISTERED' && (
          <VerdictBanner verdict="noPermit" message={`Resident car from unit ${unitNumber}. ${NO_PERMIT_ACTION}`} />
        )}
        {status === 'UNKNOWN' && (
          <VerdictBanner verdict="notRegistered" message={`No resident at ${propertyName} has this plate.`} />
        )}
      </Box>

      <Stack spacing={1.75} sx={{ pt: 2 }}>
        <Typography sx={plateStyle}>{plate}</Typography>

        {status === 'RESIDENT_REGISTERED' && <FactList facts={[...carFacts, ['Parking', 'Nothing active']]} />}

        {status === 'RESIDENT_AUTHORIZED' && (
          <>
            <FactList facts={carFacts} />
            <Stack spacing={0.75}>
              {authorizations.map((auth) => (
                <Box key={auth.spotLabel ?? 'permit'} sx={{ bgcolor: 'background.paper', borderRadius: 1, px: 1.5, py: 1.1, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1.25 }}>
                  {/* A null spot label means the permit area, same as in the backend */}
                  <Typography sx={{ fontFamily: fonts.display, fontWeight: 600, fontSize: 19 }}>
                    {auth.spotLabel ? `Spot ${auth.spotLabel}` : 'Permit area'}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {auth.validUntil ? `until ${formatDate(auth.validUntil)}` : 'no expiry'}
                  </Typography>
                </Box>
              ))}
            </Stack>
            <Typography variant="body2" color="text.secondary">Parked anywhere else, it isn't covered.</Typography>
          </>
        )}

        {status === 'UNKNOWN' && (
          <Typography variant="body2" color="text.secondary">
            Check the plate was typed right before towing. Letters O and I are easy to mix up with 0 and 1.
          </Typography>
        )}

        {result.checkedAt && (
          <Typography variant="body2" color="text.secondary">Checked at {formatTime(result.checkedAt)}</Typography>
        )}
      </Stack>
    </>
  );
}