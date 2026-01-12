import { getCookie, createError } from 'h3';

export default defineEventHandler(async (event) => {
  const token = getCookie(event, 'token');
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const { lat, lon } = getQuery(event);
  if (!lat || !lon) {
    throw createError({ statusCode: 400, statusMessage: 'Latitud y longitud son requeridos' });
  }
  try {
    const data = await $fetch(`http://localhost:8080/cercanos/${lat}/${lon}`, {
      method: 'GET',
      headers
    });
    return data;
  } catch (err: any) {
    console.error('[drones proxy] error fetching backend', err);
    throw createError({ 
      statusCode: err?.statusCode || 502, 
      statusMessage: err?.data?.message || String(err) 
    });
  }
});
