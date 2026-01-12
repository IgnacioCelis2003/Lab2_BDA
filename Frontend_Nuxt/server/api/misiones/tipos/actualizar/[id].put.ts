import { getCookie, createError, readBody } from "h3";

export default defineEventHandler(async (event) => {
  const id = event.context.params?.id;
  if (!id) {
    throw createError({ statusCode: 400, statusMessage: "Missing tipo id" });
  }

  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    const body = await readBody(event);

    return await $fetch(
      `http://localhost:8080/api/tipos-mision/actualizar/${id}`,
      {
        method: "PUT",
        headers,
        body,
      }
    );
  } catch (err: any) {
    console.error("[tipos-mision actualizar] error", err);
    throw createError({
      statusCode: err?.statusCode || 502,
      statusMessage: err?.data?.message || err?.message || String(err),
    });
  }
});
