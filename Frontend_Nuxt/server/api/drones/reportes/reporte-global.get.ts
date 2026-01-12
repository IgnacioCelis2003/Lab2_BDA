import { getCookie, createError } from 'h3';

export default defineEventHandler(async (event) => {
  // Leer el token de la cookie
  const token = getCookie(event, 'token');

  // Preparar los headers
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    const data = await $fetch('http://localhost:8080/api/drones/reportes/global', {
      method: 'GET',
      headers
    });

    return data;

  } catch (err: any) {
    console.error('[duracion-vuelo proxy] error fetching backend', err);
    throw createError({ 
      statusCode: err?.statusCode || 502, 
      statusMessage: err?.data?.message || String(err) 
    });
  }
});