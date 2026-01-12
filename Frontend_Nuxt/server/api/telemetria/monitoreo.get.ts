import { getCookie, createError } from 'h3';

export default defineEventHandler(async (event) => {
  // Leer token httpOnly de la cookie
  const token = getCookie(event, 'token');

  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    // Llamar al backend y reenviar el header Authorization si tenemos token
    const data = await $fetch('http://localhost:8080/api/telemetria/monitoreo', {
      method: 'GET',
      headers
    });
    return data;
  } catch (err: any) {
    console.error('[drones proxy] error fetching backend', err);
    throw createError({ statusCode: err?.statusCode || 502, statusMessage: err?.data?.message || String(err) });
  }
});
