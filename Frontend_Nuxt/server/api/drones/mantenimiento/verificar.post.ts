import { getCookie, readBody, createError } from 'h3';

export default defineEventHandler(async (event) => {
  const token = getCookie(event, 'token');
  const body = await readBody(event);
  const idModelo = body.idModelo;

  if (!idModelo) {
    throw createError({ statusCode: 400, statusMessage: 'ID de modelo requerido' });
  }

  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    // Llamamos al endpoint del Controller
    const response = await $fetch(`http://localhost:8080/api/drones/mantenimiento/modelo/${idModelo}`, {
      method: 'POST',
      headers
    });
    return response;

  } catch (err: any) {
    throw createError({ 
      statusCode: err?.statusCode || 500, 
      statusMessage: err?.data?.message || 'Error ejecutando mantenimiento masivo' 
    });
  }
});