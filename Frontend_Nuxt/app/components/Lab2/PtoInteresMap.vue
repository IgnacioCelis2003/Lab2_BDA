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

// --- CAMBIO CLAVE: NO USAR REF PARA EL MAPA ---
// Vue Proxy rompe Leaflet. Usamos variables nativas de JS.
let mapInstance = null;
let L = null;
let markers = {};
let rutaLayer = null;

const pois = ref([]); // Los datos sí pueden ser reactivos

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
  // Tamaños más pequeños y discretos
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
// En components/Lab2/PtoInteresMap.vue

async function dibujarRuta(id) {
  // Si no hay mapa o librería cargada, abortamos para no causar errores
  if (!id || !mapInstance || !L) return;

  // Limpiar ruta anterior si existe
  if (rutaLayer) {
    mapInstance.removeLayer(rutaLayer);
    rutaLayer = null;
  }

  try {
    const data = await $fetch(`/api/telemetria/mision/${id}`);

    // VALIDACIÓN 1: ¿Vino data?
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

    // TRUCO DE MAGIA: Invalidar tamaño para forzar repintado
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
/* FORZAR ALTURA ES CRÍTICO */
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
</style>
