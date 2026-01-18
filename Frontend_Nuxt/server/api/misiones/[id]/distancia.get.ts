// server/api/misiones/[id]/distancia.get.ts
import { getCookie, createError, getRouterParam } from "h3";

export default defineEventHandler(async (event) => {
  // 1. Obtener el ID de la misión desde la URL de Nuxt
  const idMision = getRouterParam(event, "id");
  const token = getCookie(event, "token");

  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    // 2. Llamar al endpoint de Spring Boot que definimos antes
    // URL Java: /api/misiones/{id}/estadisticas/distancia-recorrida
    const data = await $fetch(
      `http://localhost:8080/api/misiones/${idMision}/estadisticas/distancia-recorrida`,
      {
        method: "GET",
        headers,
      },
    );
    return data;
  } catch (err: any) {
    console.error("[drones proxy] error calculando distancia", err);
    throw createError({
      statusCode: err?.statusCode || 502,
      statusMessage: err?.data?.message || String(err),
    });
  }
});
