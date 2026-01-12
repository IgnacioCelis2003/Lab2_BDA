import { getCookie, createError, readBody } from "h3";

export default defineEventHandler(async (event) => {
  const id = event.context.params?.id;
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: "Missing mision id" });
  }

  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    const body = await readBody(event);

    // OJO: backend real (NO /api/..., evita loop)
    const data = await $fetch(
      `http://localhost:8080/api/misiones/iniciar/${id}`,
      {
        method: "POST",
        headers,
        body,
      }
    );

    return data;
  } catch (err: any) {
    console.error("[misiones actualizar] error", err);
    throw createError({
      statusCode: err?.statusCode || 502,
      statusMessage: err?.data?.message || err?.message || String(err),
    });
  }
});