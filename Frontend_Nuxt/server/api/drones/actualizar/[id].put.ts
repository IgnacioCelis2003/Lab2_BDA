import { getCookie, createError, readBody } from 'h3';

export default defineEventHandler(async (event): Promise<any> => {
  const id = event.context.params?.id;
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: 'Missing drone id' });
  }

  const token = getCookie(event, 'token');
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    const body = await readBody(event);
    const data: any = await $fetch(`http://localhost:8080/api/drones/actualizar/${id}`, {
      method: 'PUT',
      headers,
      body
    });
    return data;
  } catch (err: any) {
    console.error('[modelos proxy] error fetching backend', err);
    throw createError({ statusCode: err?.statusCode || 502, statusMessage: err?.data?.message || String(err) });
  }
});