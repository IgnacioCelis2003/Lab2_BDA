import { getCookie, createError } from "h3";

export default defineEventHandler(async (event) => {
  const id = event.context.params?.id;
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: "Missing tipo id" });
  }

  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    await $fetch(`http://localhost:8080/api/tipos-mision/eliminar/${id}`, {
      method: "DELETE",
      headers,
    });

    return { ok: true };
  } catch (err: any) {
    console.error("[tipos-mision eliminar] error", err);
    throw createError({
      statusCode: err?.statusCode || 502,
      statusMessage: err?.data?.message || err?.message || String(err),
    });
  }
});
