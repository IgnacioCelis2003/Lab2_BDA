import { getCookie, createError } from 'h3';

export default defineEventHandler(async (event) => {
  const body = await readBody(event as any);

  if (!body) {
    throw createError({ statusCode: 400, statusMessage: 'Missing body' });
  }

  console.log('[zonas crear proxy] body recibido:', JSON.stringify(body));

  // Extract token from httpOnly cookie
  const token = getCookie(event, 'token');
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    console.log('[zonas crear proxy] enviando al backend:', {
      nombre: body.nombre,
      wkt: body.wkt
    });
    
    const resp = await $fetch('http://localhost:8080/api/zonas/crear', {
      method: 'POST',
      body,
      headers
    });
    
    console.log('[zonas crear proxy] respuesta exitosa:', resp);
    
    // Transform the response to convert area (JTS geometry) to WKT format
    if (resp && typeof resp === 'object') {
      return {
        ...resp,
        area: resp.wkt || (resp.area ? convertJTStoWKT(resp.area) : '')
      };
    }
    
    return resp;
  } catch (err: any) {
    console.error('[zonas crear proxy] error:', err?.data || err?.message);
    throw createError({ 
      statusCode: err?.statusCode || 502, 
      statusMessage: err?.data?.message || err?.data || err?.message || 'Error en el servidor' 
    });
  }
});

function convertJTStoWKT(jtsObj: any): string {
  try {
    const coords = extractCoordinatesFromJTS(jtsObj);
    if (coords.length > 0) {
      const wktCoords = coords.map(([lat, lon]) => `${lon} ${lat}`).join(', ');
      const closedCoords = wktCoords + ', ' + coords.map(([lat, lon]) => `${lon} ${lat}`)[0];
      return `POLYGON((${closedCoords}))`;
    }
  } catch (error) {
    console.error('Error converting JTS to WKT:', error);
  }
  return '';
}

function extractCoordinatesFromJTS(jtsObj: any): [number, number][] {
  const coordinates: [number, number][] = [];
  
  const traverse = (obj: any): void => {
    if (!obj || typeof obj !== 'object') return;
    
    if (obj.coordinates && Array.isArray(obj.coordinates)) {
      obj.coordinates.forEach((coord: any) => {
        if (Array.isArray(coord) && coord.length >= 2) {
          coordinates.push([coord[1], coord[0]]);
        }
      });
    }
    
    for (const key in obj) {
      if (obj[key] && typeof obj[key] === 'object' && !coordinates.length) {
        traverse(obj[key]);
      }
    }
  };
  
  traverse(jtsObj);
  return coordinates;
}