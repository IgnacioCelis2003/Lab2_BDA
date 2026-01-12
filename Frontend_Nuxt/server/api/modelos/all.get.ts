import { getCookie, createError } from 'h3';

export default defineEventHandler(async (event) => {
  const token = getCookie(event, 'token');
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    const data = await $fetch('http://localhost:8080/api/modelos', {
      method: 'GET',
      headers
    });
    return data;
  } catch (err: any) {
    console.error('[modelos proxy] error fetching backend', err);
    throw createError({ statusCode: err?.statusCode || 502, statusMessage: err?.data?.message || String(err) });
  }
});