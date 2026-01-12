import { readBody, setCookie, createError } from 'h3';

export default defineEventHandler(async (event): Promise<any> => {
  const body = await readBody(event as any);

  console.log('[login proxy] incoming request body:', body);
  // Log some request headers for debugging (origin, host, cookie)
  try {
    const reqHeaders = (event as any)?.node?.req?.headers || {};
    console.log('[login proxy] request headers:', {
      origin: reqHeaders.origin,
      host: reqHeaders.host,
      cookie: reqHeaders.cookie
    });
  } catch (e) {
    console.log('[login proxy] could not read request headers', String(e));
  }

  if (!body || !body.email || !body.password) {
    console.warn('[login proxy] missing credentials in body');
    throw createError({ statusCode: 400, message: 'Se requieren email y password' });
  }

  // Llamamos al backend directamente en el puerto 8080
  let resp: any = null;
  try {
    resp = await $fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      body: {
        email: body.email,
        password: body.password
      },
      // No reintentar ni transformar
      credentials: 'omit'
    });

    console.log('[login proxy] backend responded:', resp);
  } catch (err: any) {
    // Log detailed error info
    try {
      console.error('[login proxy] backend fetch error - err:', err);
      // if $fetch provides status/data
      console.error('[login proxy] backend fetch error - statusCode:', err?.statusCode, 'data:', err?.data || err?.body);
    } catch (ee) {
      console.error('[login proxy] error logging failed', ee);
    }
    // Re-throw a controlled error to the client with info
    throw createError({ statusCode: err?.statusCode || 502, message: err?.data?.message || String(err) });
  }

  // Se espera que el backend devuelva { token: '...' }
  const token = resp && resp.token ? resp.token : null;

  if (!token) {
    console.warn('[login proxy] backend did not return token, resp:', resp);
    throw createError({ statusCode: 500, message: 'No se recibió token del backend' });
  }

  // Fijar cookie httpOnly segura (en dev: secure=false si no HTTPS)
  const cookieOptions = {
    httpOnly: false,
    path: '/',
    // En producción, ponga secure: true y sameSite según su dominio
    secure: false,
    maxAge: 60 * 60 * 24 * 7 // 7 días
  } as any;

  setCookie(event, 'token', token, cookieOptions);

  // Retornamos sólo un éxito
  return { success: true };
});
