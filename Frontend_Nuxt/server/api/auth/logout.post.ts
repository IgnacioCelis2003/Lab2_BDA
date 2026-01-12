export default defineEventHandler(async (event) => {
  // Borrar cookie estableciendo maxAge=0
  try {
    // @ts-ignore
    setCookie(event, 'token', '', { path: '/', maxAge: 0 });
  } catch (e) {
    // ignore
  }

  return { success: true };
});
