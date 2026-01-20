export default defineEventHandler(async (event) => {
  const body = await readBody(event);
  const config = useRuntimeConfig();
  const token = getCookie(event, "token");

  // 1. CONSTRUIR LA URL
  const backendUrl = `http://localhost:8080/api/puntos-interes/crear`;

  // --- DEBUG 1: ¿A DÓNDE VAMOS? ---
  console.log("-------------------------------------------------");
  console.log("🚀 INTENTO DE CONEXIÓN BACKEND");
  console.log("📍 URL Objetivo:", backendUrl);
  console.log("📦 Body enviado:", JSON.stringify(body, null, 2));
  console.log("🔑 Token presente:", token ? "SÍ" : "NO");
  console.log("-------------------------------------------------");

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    const response = await $fetch(backendUrl, {
      method: "POST",
      body,
      headers,
    });
    return response;
  } catch (error: any) {
    // --- DEBUG 2: EL ERROR CRUDO ---
    console.error("❌ ERROR DETECTADO:");
    // Imprimimos el error completo, no solo .data
    console.error(error);

    // Si es un error de respuesta, intentamos leer el texto aunque no sea JSON
    if (error.response) {
      console.error("Status:", error.response.status);
      console.error("Status Text:", error.response.statusText);
      // A veces el servidor devuelve HTML (como un error de nginx), esto nos lo mostrará
      console.error("Response Body (Texto):", error.response._data);
    }

    throw createError({
      statusCode: error.response?.status || 500,
      statusMessage: "Revisa la terminal de VS Code (Error Logs)",
      data: error.data || "Error desconocido",
    });
  }
});