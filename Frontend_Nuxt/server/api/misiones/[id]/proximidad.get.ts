// server/api/misiones/[id]/proximidad.get.ts
import { getCookie, createError, getRouterParam, getQuery } from "h3";

export default defineEventHandler(async (event) => {
  // 1. Obtener ID Misión de la ruta de Nuxt
  const idMision = getRouterParam(event, "id");

  // 2. Obtener ID POI de los query params que envía el Frontend
  // (El frontend seguirá enviando ?poiId=... porque es más fácil de manejar allí)
  const query = getQuery(event);
  const idPoi = query.poiId;

  if (!idMision || !idPoi) {
    throw createError({ statusCode: 400, statusMessage: "Faltan IDs" });
  }

  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    // 3. CONSTRUIR LA URL EXACTA DE TU CONTROLLER
    // Controller: @GetMapping("/{idMision}/proximidad-poi/{idPoi}")
    // Base: http://localhost:8080/api/misiones
    const urlJava = `http://localhost:8080/api/misiones/${idMision}/proximidad-poi/${idPoi}`;

    const data = await $fetch(urlJava, {
      method: "GET",
      headers,
    });

    return data;
  } catch (err: any) {
    console.error("Error proxy proximidad:", err);
    throw createError({
      statusCode: err?.statusCode || 500,
      statusMessage: err?.data?.message || "Error conectando con Backend",
    });
  }
});
