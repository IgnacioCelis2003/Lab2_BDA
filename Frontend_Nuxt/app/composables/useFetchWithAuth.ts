export function useFetchWithAuth(url: string, opts: any = {}) {
  // Always call our server API proxy which will include the httpOnly cookie
  // If url already starts with /api we call it directly, otherwise prefix with /api
  const target = url.startsWith('/api') ? url : `/api${url}`;
  return useFetch(target, opts);
}
