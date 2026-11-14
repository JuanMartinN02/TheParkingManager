import { useState } from 'react';
import { AppBar, Box, Button, ButtonBase, Menu, MenuItem, Toolbar, Typography } from '@mui/material';
import { Outlet } from 'react-router-dom';
import { useProperty } from '../auth/PropertyContext';
import { useSession } from '../auth/SessionContext';
import { fonts } from '../theme/theme';
import { ChevronDownIcon } from './icons';
import { LogoBadge } from './LogoBadge';

const placeNameStyle = { fontFamily: fonts.display, fontWeight: 600, fontSize: 19, lineHeight: 1.1 };

// The blue bar on every signed-in screen. Pages render where <Outlet /> is
export function AppShell() {
  const { signOut } = useSession();
  const { properties, property, selectProperty } = useProperty();
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);

  // Only people who work on several properties get a picker
  const canPick = properties.length > 1;

  return (
    <>
      <AppBar position="sticky">
        <Toolbar sx={{ gap: 1.5 }}>
          <LogoBadge />
          <Box sx={{ minWidth: 0 }}>
            <Typography sx={{ fontSize: 12, opacity: 0.8, lineHeight: 1.2 }}>Property</Typography>
            {canPick ? (
              <ButtonBase onClick={(e) => setMenuAnchor(e.currentTarget)} aria-haspopup="menu" sx={{ ...placeNameStyle, gap: 0.5 }}>
                {property?.name}
                <ChevronDownIcon />
              </ButtonBase>
            ) : (
              <Typography noWrap sx={placeNameStyle}>{property?.name ?? 'ESW Parking Management'}</Typography>
            )}
          </Box>
          <Button color="inherit" onClick={() => void signOut()} sx={{ ml: 'auto', flex: 'none' }}>
            Sign out
          </Button>
        </Toolbar>
      </AppBar>

      <Menu anchorEl={menuAnchor} open={menuAnchor !== null} onClose={() => setMenuAnchor(null)}>
        {properties.map((p) => (
          <MenuItem key={p.id} selected={p.id === property?.id}
                    onClick={() => { if (p.id) selectProperty(p.id); setMenuAnchor(null); }}>
            {p.name}
          </MenuItem>
        ))}
      </Menu>

      <Box component="main" sx={{ p: 2 }}>
        <Outlet />
      </Box>
    </>
  );
}