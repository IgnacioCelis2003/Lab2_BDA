export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, "id");
  const config = useRuntimeConfig();

  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    await $fetch(`http://localhost:8080/api/puntos-interes/eliminar/${id}`, {
      method: "DELETE",
      headers,
    });
    return { success: true };
  } catch (error: any) {
    throw createError({
      statusCode: error.response?.status || 500,
      statusMessage: "No se pudo eliminar el punto",
    });
  }
});
