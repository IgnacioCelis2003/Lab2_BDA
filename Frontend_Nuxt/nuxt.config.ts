// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  modules: ["@nuxt/eslint"],
  devtools: { enabled: true },
  compatibilityDate: "2025-07-15",
  css: ["@picocss/pico", "leaflet/dist/leaflet.css"],
  eslint: {
    config: {
      stylistic: {
        semi: true,
        quotes: "double",
      },
    },
  },

  app: {
    head: {
      link: [
        {
          rel: "stylesheet",
          href: "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css",
          integrity: "sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=",
          crossorigin: "",
        },
      ],
    },
  },
});
