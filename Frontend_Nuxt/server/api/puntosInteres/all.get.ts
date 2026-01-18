// server/api/puntosInteres/all.get.ts
import { getCookie } from "h3";

export default defineEventHandler(async (event) => {
  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    return await $fetch("http://localhost:8080/api/puntos-interes", {
      method: "GET",
      headers,
    });
  } catch (err) {
    return [];
  }
});
