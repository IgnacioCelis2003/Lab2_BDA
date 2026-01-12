import { getCookie, createError } from 'h3';

export default defineEventHandler(async (event) => {
  // Leer el token de la cookie
  const token = getCookie(event, 'token');

  // Preparar los headers
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    // Llamar al Backend y reenviar el header Authorization si tenemos token
    const data = await $fetch('http://localhost:8080/api/misiones/reportes/consumo-bateria', {
      method: 'GET',
      headers
    });

    return data;

  } catch (err: any) {
    console.error('[desempeno-tipo-mision proxy] error fetching backend', err);
    throw createError({ 
      statusCode: err?.statusCode || 502, 
      statusMessage: err?.data?.message || String(err) 
    });
  }
});