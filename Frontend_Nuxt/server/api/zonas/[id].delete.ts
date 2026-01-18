import { getCookie, createError } from 'h3';

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id');
  const token = getCookie(event, 'token');
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    const response = await $fetch(`http://localhost:8080/api/zonas/eliminar/${id}`, {
      method: 'DELETE',
      headers
    });
    return response;
  } catch (err: any) {
    console.error('[zonas delete proxy] error', err);
    throw createError({ statusCode: err?.statusCode || 502, statusMessage: err?.data?.message || String(err) });
  }
});
