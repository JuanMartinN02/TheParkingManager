import { useEffect, useState, type FormEvent } from 'react';
import { Box, Button, CircularProgress, Stack, TextField, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { ApiError } from '../../api/client';
import { lookupPlate, lookupSpot, type PlateLookup, type SpotLookup } from '../../api/lookup';
import { useProperty } from '../../auth/PropertyContext';
import { FormError } from '../../components/FormError';
import { fonts } from '../../theme/theme';
import { PlateResult } from './PlateResult';
import { SpotResult } from './SpotResult';

type Mode = 'plate' | 'spot';

export function LookupPage() {
  const { property, loading, error: propertyError } = useProperty();
  const [mode, setMode] = useState<Mode>('plate');
  const [query, setQuery] = useState('');
  const [plateResult, setPlateResult] = useState<PlateLookup | null>(null);
  const [spotResult, setSpotResult] = useState<SpotLookup | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function clearResults() {
    setPlateResult(null);
    setSpotResult(null);
    setError(null);
  }

  // An answer from one property must never stay on screen after switching to another
  useEffect(clearResults, [property?.id]);

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 6 }} />;
  if (!property?.id) return <FormError message={propertyError ?? 'There is no property to check plates in.'} />;
  const propertyId = property.id;

  function changeMode(_event: unknown, next: Mode | null) {
    // Clicking the already selected option sends null: keep the current mode
    if (!next) return;
    setMode(next);
    setQuery('');
    clearResults();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!query.trim()) return;
    // The old answer disappears first, so it can't be mistaken for the new plate's answer
    clearResults();
    setBusy(true);
    try {
      if (mode === 'plate') setPlateResult(await lookupPlate(propertyId, query));
      else setSpotResult(await lookupSpot(propertyId, query));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Can't reach the server. Don't tow until the check works.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Box sx={{ maxWidth: 520, mx: 'auto' }}>
      <ToggleButtonGroup exclusive fullWidth value={mode} onChange={changeMode} aria-label="What to check" sx={{ mb: 1.75 }}>
        <ToggleButton value="plate">Plate</ToggleButton>
        <ToggleButton value="spot">Spot</ToggleButton>
      </ToggleButtonGroup>

      <Stack component="form" direction="row" spacing={1} onSubmit={handleSubmit}>
        <TextField
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder={mode === 'plate' ? 'ABC 1234' : 'B1'}
          autoFocus
          // Plates are letters and numbers: no autocorrect or spell check fighting the operator
          slotProps={{ htmlInput: { 'aria-label': mode === 'plate' ? 'Plate' : 'Spot', autoCapitalize: 'characters', autoCorrect: 'off', autoComplete: 'off', spellCheck: false } }}
          sx={{ flex: 1, '& input': { fontFamily: fonts.display, fontWeight: 700, fontSize: 34, letterSpacing: '0.05em', textTransform: 'uppercase', py: 1.25 } }}
        />
        <Button type="submit" variant="contained" disabled={busy || !query.trim()} sx={{ px: 2.5, fontSize: 19, flex: 'none' }}>
          {busy ? 'Checking' : 'Check'}
        </Button>
      </Stack>

      {error && <Box sx={{ mt: 2 }}><FormError message={error} /></Box>}
      {plateResult && <PlateResult result={plateResult} propertyName={property.name ?? 'this property'} />}
      {spotResult && <SpotResult result={spotResult} />}
    </Box>
  );
}