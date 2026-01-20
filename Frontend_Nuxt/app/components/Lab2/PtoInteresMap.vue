<template>
  <div
    class="map-container"
    style="height: 100%; width: 100%; position: relative"
  >
    <link
      rel="stylesheet"
      href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
      integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
      crossorigin=""
    />

    <div
      ref="mapContainer"
      style="height: 100%; width: 100%; min-height: 300px; background: #eee"
    ></div>

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
  // Agregamos esto para evitar warnings si le pasas puntos vacíos desde el padre
  puntos: { type: Array, default: () => [] },
});

// 1. DEFINIR EMITS (Para avisar al padre del clic) <--- NUEVO
const emit = defineEmits(["map-click"]);

const { misionId, poiId } = toRefs(props);

// Referencia al div del mapa (Reemplaza al id="map-poi") <--- NUEVO
const mapContainer = ref(null);

// Vue Proxy rompe Leaflet. Usamos variables nativas de JS.
let mapInstance = null;
let L = null;
let markers = {};
let rutaLayer = null;
let rutaVisualLayer = null;
let rutaMarkers = [];

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
  const color = isSelected ? "#dc2626" : "#2563eb";
  const size = isSelected ? 32 : 24;

  const svgHtml = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="${size}" height="${size}" style="filter: drop-shadow(0 2px 2px rgba(0,0,0,0.3)); transition: all 0.2s ease-in-out;">
      <path fill="${color}" stroke="white" stroke-width="1.5" d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
      <circle cx="12" cy="10" r="3" fill="white"/>
    </svg>
  `;

  return L.divIcon({
    className: isSelected ? "leaflet-marker-selected" : "leaflet-marker-normal",
    html: svgHtml,
    iconSize: [size, size],
    iconAnchor: [size / 2, size],
    popupAnchor: [0, -size],
  });
}

// --- CARGAR POIS ---
async function fetchPois() {
  try {
    const data = await $fetch("/api/puntosInteres/all");
    pois.value = data || [];

    if (!mapInstance || !L) return;

    Object.values(markers).forEach((m) => mapInstance.removeLayer(m));
    markers = {};

    pois.value.forEach((p) => {
      const coords = parseWktToLatLng(p.ubicacionWKT);
      if (coords) {
        const marker = L.marker(coords, { icon: getIcon("normal") }).addTo(
          mapInstance,
        );
        
        // Extraer altitud del WKT si existe (formato: POINT Z(lon lat alt))
        let altitud = null;
        if (p.ubicacionWKT) {
          const clean = p.ubicacionWKT.replace(/[^\d\s.-]/g, "").trim();
          const parts = clean.split(/\s+/);
          if (parts.length >= 3) {
            altitud = parseFloat(parts[2]);
          }
        }
        
        // Popup con coordenadas completas
        const popupContent = `
          <div style="min-width: 180px;">
            <h4 style="margin: 0 0 8px 0; color: #1e40af; font-size: 1rem;">${p.nombre}</h4>
            ${p.descripcion ? `<p style="margin: 0 0 8px 0; color: #475569; font-size: 0.85rem;">${p.descripcion}</p>` : ''}
            <hr style="margin: 8px 0; border: 0; border-top: 1px solid #e2e8f0;">
            <div style="font-size: 0.8rem; color: #334155;">
              <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                <span>📍 Latitud:</span>
                <strong>${coords[0].toFixed(6)}</strong>
              </div>
              <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                <span>📍 Longitud:</span>
                <strong>${coords[1].toFixed(6)}</strong>
              </div>
              <div style="display: flex; justify-content: space-between;">
                <span>⛰️ Altitud:</span>
                <strong>${altitud !== null && !isNaN(altitud) ? altitud.toFixed(1) + ' m' : 'N/A'}</strong>
              </div>
            </div>
          </div>
        `;
        
        marker.bindPopup(popupContent);
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
  if (rutaLayer) {
    mapInstance.removeLayer(rutaLayer);
    rutaLayer = null;
  }

  try {
    const data = await $fetch(`/api/telemetria/mision/${id}`);
    if (!data || !Array.isArray(data) || data.length === 0) return;

    const latlngs = data
      .filter((d) => d.latitud != null && d.longitud != null)
      .map((d) => [d.latitud, d.longitud]);

    if (latlngs.length > 0) {
      rutaLayer = L.polyline(latlngs, { color: "red", weight: 4 }).addTo(
        mapInstance,
      );
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

  if (rutaVisualLayer) {
    mapInstance.removeLayer(rutaVisualLayer);
    rutaVisualLayer = null;
  }
  rutaMarkers.forEach((m) => mapInstance.removeLayer(m));
  rutaMarkers = [];

  try {
    const puntos = await $fetch(`/api/misiones/${idMision}/ruta`);
    if (!puntos || puntos.length < 2) return;

    const latlngs = puntos.map((p) => [p.latitud, p.longitud]);

    const crearIconoSVG = (emoji) => {
      const svgString = `
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
          <text y="50%" x="50%" dominant-baseline="middle" text-anchor="middle" font-size="80">${emoji}</text>
        </svg>`;
      const urlImagen =
        "data:image/svg+xml;base64," +
        btoa(unescape(encodeURIComponent(svgString)));
      return L.icon({
        iconUrl: urlImagen,
        iconSize: [40, 40],
        iconAnchor: [20, 20],
        popupAnchor: [0, -20],
      });
    };

    const inicioIcon = crearIconoSVG("🛸");
    const finIcon = crearIconoSVG("🏁");

    rutaVisualLayer = L.polyline(latlngs, {
      color: "#8B0000",
      weight: 5,
      opacity: 0.9,
      dashArray: "10, 10",
      lineCap: "round",
    }).addTo(mapInstance);

    const markerInicio = L.marker(latlngs[0], {
      icon: inicioIcon,
      zIndexOffset: 1000,
    }).addTo(mapInstance);
    const markerFin = L.marker(latlngs[latlngs.length - 1], {
      icon: finIcon,
      zIndexOffset: 1000,
    }).addTo(mapInstance);

    rutaMarkers.push(markerInicio, markerFin);
    mapInstance.fitBounds(rutaVisualLayer.getBounds(), { padding: [50, 50] });
  } catch (e) {
    console.error("Error dibujando ruta visual:", e);
  }
}

defineExpose({ dibujarRutaVisual });

// --- WATCHERS ---
watch(poiId, (newId) => {
  if (!mapInstance || !L) return;
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
  // Esperamos un poco más para asegurar que el DOM pintó el div
  await new Promise((r) => setTimeout(r, 200));

  try {
    const leafletModule = await import("leaflet");
    L = leafletModule.default || leafletModule;

    if (mapContainer.value) {
      // 1. Inicializar mapa
      mapInstance = L.map(mapContainer.value).setView([-33.437, -70.65], 10);

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "© OpenStreetMap",
      }).addTo(mapInstance);

      mapInstance.on("click", (e) => {
        const { lat, lng } = e.latlng;
        emit("map-click", { lat, lng });
      });

      // --- LA SOLUCIÓN DEFINITIVA PARA EL MAPA BLANCO ---
      // Leaflet necesita saber que el div cambió de tamaño.
      // Lo forzamos varias veces por si acaso hay animaciones de carga.
      setTimeout(() => {
        mapInstance.invalidateSize();
      }, 100);
      setTimeout(() => {
        mapInstance.invalidateSize();
      }, 500);
      setTimeout(() => {
        mapInstance.invalidateSize();
      }, 1000);

      await fetchPois();

      if (misionId.value) dibujarRuta(misionId.value);
    }
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
