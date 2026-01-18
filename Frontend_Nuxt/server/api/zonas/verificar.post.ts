export default defineEventHandler(async (event) => {
  const body = await readBody(event);
  const backendUrl = process.env.NUXT_PUBLIC_API_URL || 'http://localhost:8080';
  
  try {
    const response = await $fetch(`${backendUrl}/api/zonas/verificar-ruta`, {
      method: 'POST',
      body: {
        rutaWkt: body.rutaWkt
      }
    });

    return response;
  } catch (error) {
    console.error('Error verificando ruta:', error);
    throw createError({
      statusCode: 500,
      statusMessage: 'Error al verificar la ruta'
    });
  }
});
