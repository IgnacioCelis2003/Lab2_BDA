export const useAuth = () => {
  const error = ref<string | null>(null);
  const loading = ref(false);

  async function login(email: string, password: string) {
    error.value = null;
    loading.value = true;
    try {
      const res = await $fetch('/api/auth/login', {
        method: 'POST',
        body: { email, password }
      });
      loading.value = false;
      return res;
    } catch (e: any) {
      loading.value = false;
      error.value = e?.data?.message || e?.message || 'Error de autenticación';
      throw e;
    }
  }

  async function logout() {
    try {
  await $fetch('/api/auth/logout', { method: 'POST' });
    } catch (e) {
      // ignore
    }
  }

  return { login, logout, error, loading };
};
