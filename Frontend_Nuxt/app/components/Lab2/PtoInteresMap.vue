<template>
  <div class="map-container">
    <link
      rel="stylesheet"
      href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
      integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
      crossorigin=""
    />

    <div id="map-poi" class="map-content"></div>

    <div class="legend-overlay">
      <div class="legend-item">
        <span class="dot poi"></span> Puntos de Interés
      </div>
      <div v-if="rutaLayer" class="legend-item">
        <span class="dot route"></span> Ruta de Misión
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, toRefs, onUnmounted } from "vue";

const props = defineProps({
  misionId: { type: [Number, String], default: null },
  poiId: { type: [Number, String], default: null },
});

const { misionId, poiId } = toRefs(props);

// Vue Proxy rompe Leaflet. Usamos variables nativas de JS.
let mapInstance = null;
let L = null;
let markers = {};
let rutaLayer = null;
let rutaVisualLayer = null; // Capa para la línea neón
let rutaMarkers = []; // Array para guardar los pines de inicio/fin

const pois = ref([]);

// --- PARSER WKT ---
function parseWktToLatLng(wkt) {
  if (!wkt) return null;
  const clean = wkt.replace(/[^\d\s.-]/g, "").trim();
  const parts = clean.split(/\s+/);
  if (parts.length >= 2) {
    const lon = parseFloat(parts[0]);
    const lat = parseFloat(parts[1]);
    if (isNaN(lat) || isNaN(lon)) return null;
    return [lat, lon];
  }
  return null;
}

// --- ICONOS SVG PROFESIONALES ---
function getIcon(tipo) {
  if (!L) return null;

  const isSelected = tipo === "selected";

  // Colores más modernos (Azul y Rojo vibrante)
  const color = isSelected ? "#dc2626" : "#2563eb";

  const size = isSelected ? 32 : 24;

  // SVG del pin de mapa (con un círculo blanco dentro y borde blanco para contraste)
  const svgHtml = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="${size}" height="${size}" style="filter: drop-shadow(0 2px 2px rgba(0,0,0,0.3)); transition: all 0.2s ease-in-out;">
      <path fill="${color}" stroke="white" stroke-width="1.5" d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
      <circle cx="12" cy="10" r="3" fill="white"/>
    </svg>
  `;

  return L.divIcon({
    // Usamos una clase específica para resetear el CSS
    className: isSelected ? "leaflet-marker-selected" : "leaflet-marker-normal",
    html: svgHtml,
    iconSize: [size, size],
    // El ancla está abajo al centro (la punta del pin)
    iconAnchor: [size / 2, size],
    // El popup se abre arriba del pin
    popupAnchor: [0, -size],
  });
}

// --- CARGAR POIS ---
async function fetchPois() {
  try {
    const data = await $fetch("/api/puntosInteres/all");
    pois.value = data || [];

    if (!mapInstance || !L) return;

    // Limpiar marcadores viejos
    Object.values(markers).forEach((m) => mapInstance.removeLayer(m));
    markers = {};

    pois.value.forEach((p) => {
      const coords = parseWktToLatLng(p.ubicacionWKT);
      if (coords) {
        const marker = L.marker(coords, { icon: getIcon("normal") }).addTo(
          mapInstance,
        );
        marker.bindPopup(`<b>${p.nombre}</b>`);
        markers[p.poi_id] = marker;
      }
    });
  } catch (e) {
    console.error(e);
  }
}

// --- DIBUJAR RUTA ---

async function dibujarRuta(id) {
  if (!id || !mapInstance || !L) return;
  // Limpiar ruta vieja
  if (rutaLayer) {
    mapInstance.removeLayer(rutaLayer);
    rutaLayer = null;
  }

  try {
    const data = await $fetch(`/api/telemetria/mision/${id}`);

    // VALIDACIÓN 1: Vino data?
    if (!data || !Array.isArray(data) || data.length === 0) {
      console.log("La misión no tiene puntos de telemetría.");
      return;
    }

    // VALIDACIÓN 2: Filtrar coordenadas nulas o inválidas
    // Leaflet explota si le pasas [null, null] o undefined
    const latlngs = data
      .filter((d) => d.latitud != null && d.longitud != null) // Solo puntos válidos
      .map((d) => [d.latitud, d.longitud]);

    // VALIDACIÓN 3: Asegurarnos de que quedaron puntos después de filtrar
    if (latlngs.length > 0) {
      rutaLayer = L.polyline(latlngs, { color: "red", weight: 4 }).addTo(
        mapInstance,
      );

      // VALIDACIÓN 4: fitBounds a veces falla si los puntos están muy juntos o es solo 1
      try {
        const bounds = rutaLayer.getBounds();
        if (bounds.isValid()) {
          mapInstance.fitBounds(bounds, { padding: [50, 50] });
        }
      } catch (errZoom) {
        console.warn("No se pudo ajustar el zoom a la ruta", errZoom);
      }
    }
  } catch (e) {
    console.error("Error al dibujar ruta:", e);
  }
}

async function dibujarRutaVisual(idMision) {
  if (!mapInstance || !L || !idMision) return;

  // 1. Limpieza: Borrar línea y marcadores viejos si existen
  if (rutaVisualLayer) {
    mapInstance.removeLayer(rutaVisualLayer);
    rutaVisualLayer = null;
  }
  rutaMarkers.forEach((m) => mapInstance.removeLayer(m));
  rutaMarkers = [];

  try {
    // 2. Pedir los puntos al Proxy
    const puntos = await $fetch(`/api/misiones/${idMision}/ruta`);

    if (!puntos || puntos.length < 2) {
      console.log("No hay suficientes puntos para trazar ruta.");
      return;
    }

    // 3. Convertir a formato Leaflet [lat, lon]
    const latlngs = puntos.map((p) => [p.latitud, p.longitud]);

    // 4. ESTILO CIBERPUNK
    // Una línea gruesa semi-transparente abajo (el "brillo")
    // y una línea punteada sólida encima.

    // Esto evita que Leaflet use un <div>, así que adiós al cuadrado azul.
    const crearIconoSVG = (emoji) => {
      // 1. Creamos un SVG minúsculo con el emoji dentro
      const svgString = `
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
          <text y="50%" x="50%" dominant-baseline="middle" text-anchor="middle" font-size="80">${emoji}</text>
        </svg>`;

      // 2. Lo codificamos para que el navegador lo entienda como archivo de imagen
      const urlImagen =
        "data:image/svg+xml;base64," +
        btoa(unescape(encodeURIComponent(svgString)));

      // 3. Usamos L.icon (IMAGEN) en vez de L.divIcon (CAJA HTML)
      return L.icon({
        iconUrl: urlImagen,
        iconSize: [40, 40], // Tamaño de la imagen
        iconAnchor: [20, 20], // Centro de la imagen
        popupAnchor: [0, -20],
      });
    };

    // --- Usamos la función para crear los iconos ---
    const inicioIcon = crearIconoSVG("🛸");
    const finIcon = crearIconoSVG("🏁");

    // A. LÍNEA ROJO OSCURO (#8B0000) - La mantenemos igual
    rutaVisualLayer = L.polyline(latlngs, {
      color: "#8B0000",
      weight: 5,
      opacity: 0.9,
      dashArray: "10, 10",
      lineCap: "round",
    }).addTo(mapInstance);

    // Creamos los marcadores en el mapa usando estos nuevos iconos
    const markerInicio = L.marker(latlngs[0], {
      icon: inicioIcon,
      zIndexOffset: 1000,
    })
      .addTo(mapInstance)
      .bindPopup("<strong>🛸 Inicio de Misión</strong><br>Despegue.");

    const markerFin = L.marker(latlngs[latlngs.length - 1], {
      icon: finIcon,
      zIndexOffset: 1000,
    })
      .addTo(mapInstance)
      .bindPopup("<strong>🏁 Fin de Misión</strong><br>Aterrizaje.");

    // Guardamos en el array para poder borrarlos luego
    rutaMarkers.push(markerInicio, markerFin);

    // Encuadrar el mapa para ver toda la ruta
    mapInstance.fitBounds(rutaVisualLayer.getBounds(), { padding: [50, 50] });
  } catch (e) {
    console.error("Error dibujando ruta visual:", e);
  }
}

defineExpose({
  dibujarRutaVisual,
});

// --- WATCHERS ---
watch(poiId, (newId) => {
  if (!mapInstance || !L) return;

  // Resetear iconos
  Object.values(markers).forEach((m) => m.setIcon(getIcon("normal")));

  if (newId && markers[newId]) {
    const m = markers[newId];
    m.setIcon(getIcon("selected"));
    m.openPopup();
    mapInstance.setView(m.getLatLng(), 15);
  }
});

watch(misionId, (newId) => {
  if (newId) dibujarRuta(newId);
});

// --- MOUNTED ---
onMounted(async () => {
  // Esperar un tick para asegurar que el DIV existe
  await new Promise((r) => setTimeout(r, 100));

  try {
    const leafletModule = await import("leaflet");
    L = leafletModule.default || leafletModule;

    // Crear mapa
    mapInstance = L.map("map-poi").setView([-33.437, -70.65], 10);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "© OpenStreetMap",
    }).addTo(mapInstance);

    setTimeout(() => {
      mapInstance.invalidateSize();
    }, 200);

    await fetchPois();

    if (misionId.value) dibujarRuta(misionId.value);
  } catch (error) {
    console.error("Error mapa:", error);
  }
});

onUnmounted(() => {
  if (mapInstance) mapInstance.remove();
});
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 600px; /* Altura fija en pixeles, no porcentajes */
  position: relative;
  background-color: #e5e7eb; /* Gris de fondo para ver si carga el contenedor */
  border-radius: 8px;
  overflow: hidden;
}

#map-poi {
  width: 100%;
  height: 100%;
  z-index: 1;
}

.legend-overlay {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: white;
  padding: 10px;
  border-radius: 5px;
  z-index: 1000;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.3);
}

.dot {
  width: 10px;
  height: 10px;
  display: inline-block;
  border-radius: 50%;
  margin-right: 5px;
}
.dot.poi {
  background: #2563eb;
}
.dot.route {
  background: #dc2626;
}

/* Reset para iconos de Leaflet */
:deep(.leaflet-div-icon) {
  background: transparent;
  border: none;
}

/*
  Esto
  elimina
  el
  cuadrado
  blanco
  por
  defecto
  de
  los
  DivIcons
  */
:deep(.leaflet-marker-normal),
:deep(.leaflet-marker-selected) {
  background: transparent !important;
  border: none !important;
}

/* Opcional: Un pequeño efecto al pasar el mouse por los marcadores normales */
:deep(.leaflet-marker-normal:hover svg) {
  transform: scale(1.1);
  cursor: pointer;
}

/* Asegura que el seleccionado siempre se vea un poco más grande/resaltado */
:deep(.leaflet-marker-selected svg) {
  z-index: 1000; /* Traer al frente */
}
/* Esto elimina el cuadrado blanco por defecto de los DivIcons */
:deep(.leaflet-marker-normal),
:deep(.leaflet-marker-selected) {
  background: transparent !important;
  border: none !important;
}

/* Opcional: Un pequeño efecto al pasar el mouse por los marcadores normales */
:deep(.leaflet-marker-normal:hover svg) {
  transform: scale(1.1);
  cursor: pointer;
}

/* Asegura que el seleccionado siempre se vea un poco más grande/resaltado */
:deep(.leaflet-marker-selected svg) {
  z-index: 1000; /* Traer al frente */
}

/* Esto elimina el cuadrado blanco por defecto de los DivIcons */
:deep(.leaflet-marker-normal),
:deep(.leaflet-marker-selected) {
  background: transparent !important;
  border: none !important;
}

/* Opcional: Un pequeño efecto al pasar el mouse por los marcadores normales */
:deep(.leaflet-marker-normal:hover svg) {
  transform: scale(1.1);
  cursor: pointer;
}

/* Asegura que el seleccionado siempre se vea un poco más grande/resaltado */
:deep(.leaflet-marker-selected svg) {
  z-index: 1000; /* Traer al frente */
}

/* Usamos :deep porque Leaflet crea estos elementos fuera del template de Vue */
:deep(.leaflet-div-icon.marker-limpio) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important; /* A veces el cuadrado es una sombra */
}

/* Aseguramos que el contenido interno tampoco tenga fondo */
:deep(.marker-limpio div) {
  background: transparent !important;
  border: none !important;
}
</style>

<style>
/* ESTILO GLOBAL: TARJETA BLANCA */

.leaflet-div-icon.marker-tarjeta-blanca {
  /* 1. Fondo Blanco y Bordes */
  background-color: #ffffff !important; /* Blanco puro */
  border: 2px solid #e2e8f0 !important; /* Borde gris muy suave */
  border-radius: 8px !important; /* Bordes redondeados (estético) */

  /* 2. Centrado Perfecto del Emoji */
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;

  /* 3. Tamaño del Emoji */
  font-size: 20px !important; /* Tamaño controlado, no gigante */

  /* 4. Sombra para dar profundidad (Efecto flotante) */
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06) !important;

  /* 5. Quitamos cosas raras por defecto */
  outline: none !important;
}

/* Efecto opcional: al pasar el mouse se levanta un poco */
/* (Solo funcionará si no hay otros estilos bloqueando hover) */
.leaflet-div-icon.marker-tarjeta-blanca:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1 !important;
}
</style>
