import { getCookie, createError, readBody } from "h3";

export default defineEventHandler(async (event) => {
  const token = getCookie(event, "token");
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;

  try {
    const body = await readBody(event);

    return await $fetch("http://localhost:8080/api/tipos-mision/crear", {
      method: "POST",
      headers,
      body,
    });
  } catch (err: any) {
    console.error("[tipos-mision crear] error", err);
    throw createError({
      statusCode: err?.statusCode || 502,
      statusMessage: err?.data?.message || err?.message || String(err),
    });
  }
});
