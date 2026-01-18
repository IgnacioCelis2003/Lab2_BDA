import { getCookie, createError, getRouterParam } from "h3";

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, "id");
  const token = getCookie(event, "token");

  // Si no hay token, devolvemos array vacío para no romper el mapa
  if (!token) return [];

  try {
    const data = await $fetch(
      `http://localhost:8080/api/misiones/${id}/ruta-puntos`,
      {
        method: "GET",
        headers: { Authorization: `Bearer ${token}` },
      },
    );
    return data;
  } catch (err) {
    console.error("Error obteniendo ruta visual:", err);
    return [];
  }
});
