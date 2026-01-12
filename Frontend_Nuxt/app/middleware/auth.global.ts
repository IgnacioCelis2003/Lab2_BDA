export default defineNuxtRouteMiddleware(async (to, from) => {
  // Allow free access to login, register and public assets
  const publicPaths = ['/login', '/register'];
  if (publicPaths.some(p => to.path.startsWith(p))) return;

  // First, check local cookie to avoid unnecessary network calls
  const token = useCookie('token');
  if (token && token.value) return;

  try {
    // Ensure cookies are sent when checking auth on the server
    const res = await $fetch('/api/auth/check', { credentials: 'include' });
    if (!res || !res.authenticated) {
      return navigateTo('/login');
    }
  } catch (e) {
    // If anything fails, redirect to login
    return navigateTo('/login');
  }
});
