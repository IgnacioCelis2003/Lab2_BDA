import { getCookie, createError } from 'h3';

export default defineEventHandler(async (event) => {
  const body = await readBody(event as any);

  if (!body) {
    throw createError({ statusCode: 400, statusMessage: 'Missing body' });
  }

  // Extract token from httpOnly cookie
  const token = getCookie(event, 'token');
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    const resp = await $fetch('http://localhost:8080/api/modelos/crear', {
      method: 'POST',
      body,
      headers
    });
    return resp;
  } catch (err: any) {
    console.error('[modelos crear proxy] error', err);
    throw createError({ statusCode: err?.statusCode || 502, statusMessage: err?.data?.message || String(err) });
  }
});
