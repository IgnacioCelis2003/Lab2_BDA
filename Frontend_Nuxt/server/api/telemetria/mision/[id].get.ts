// server/api/telemetria/mision/[id].get.ts
import { getCookie, createError, getRouterParam } from "h3";

export default defineEventHandler(async (event) => {
  // 1. Obtener el ID de la URL (ej: /api/telemetria/mision/5 -> id = 5)
  const idMision = getRouterParam(event, "id");

  // 2. Obtener Token
  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    // 3. Llamar al Backend Spring Boot
    // Endpoint: RegistroVueloController -> getTelemetriaPorMision
    const data = await $fetch(
      `http://localhost:8080/api/telemetria/mision/${idMision}`,
      {
        method: "GET",
        headers,
      },
    );
    return data;
  } catch (err: any) {
    console.error("[drones proxy] error fetching mission telemetry", err);
    throw createError({
      statusCode: err?.statusCode || 502,
      statusMessage: err?.data?.message || String(err),
    });
  }
});
