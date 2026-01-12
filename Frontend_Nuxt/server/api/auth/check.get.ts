import { getCookie } from 'h3';

export default defineEventHandler(async (event) => {
  try {
    const token = getCookie(event, 'token');
    return { authenticated: !!token };
  } catch (e) {
    return { authenticated: false };
  }
});
