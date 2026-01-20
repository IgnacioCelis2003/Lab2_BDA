export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, "id");
  const body = await readBody(event);
  const config = useRuntimeConfig();

  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    return await $fetch(
      `http://localhost:8080/api/puntos-interes/actualizar/${id}`,
      {
        method: "PUT",
        body,
        headers,
      },
    );
  } catch (error: any) {
    throw createError({
      statusCode: error.response?.status || 500,
      statusMessage: "Error al actualizar el punto",
    });
  }
});