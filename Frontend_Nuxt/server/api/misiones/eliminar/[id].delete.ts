import { getCookie, createError } from "h3";

export default defineEventHandler(async (event) => {
  const id = event.context.params?.id;
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: "Missing mision id" });
  }

  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    // OJO: backend real (NO /api/..., evita loop)
    await $fetch(`http://localhost:8080/api/misiones/eliminar/${id}`, {
      method: "DELETE",
      headers,
    });

    return { ok: true };
  } catch (err: any) {
    console.error("[misiones eliminar] error", err);
    throw createError({
      statusCode: err?.statusCode || 502,
      statusMessage: err?.data?.message || err?.message || String(err),
    });
  }
});
