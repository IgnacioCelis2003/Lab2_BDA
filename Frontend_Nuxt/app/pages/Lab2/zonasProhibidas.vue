<script lang="ts" setup>
import { ref, onMounted, watch, shallowRef } from "vue";
import ZonaProhibidaCreate from "~/components/Lab2/ZonaProhibidaCreate.vue";

// Importar Leaflet solo en el cliente
let L: any;

const {
  data: zonas,
  error,
  status,
  refresh,
} = await useFetch<{ id: number; nombre: string; area: string }[]>("/api/zonas/all");

const showModal = ref(false);
const deletingId = ref<number | null>(null);

// Referencias para el mapa
const mapContainer = ref<HTMLElement | null>(null);
const mapInstance = shallowRef<any>(null);
const layerGroup = shallowRef<any>(null);

// Función para convertir WKT "POLYGON((lng lat, ...))" a Array de Leaflet [lat, lng]
function parseWKT(wkt: string) {
  try {
    const coordsString = wkt.replace("POLYGON((", "").replace("))", "");
    const pairs = coordsString.split(",");
    return pairs.map(pair => {
      const [lng, lat] = pair.trim().split(" ").map(Number);
      return [lat, lng]; // Leaflet usa [lat, lng]
    });
  } catch (e) {
    console.error("Error parseando WKT:", e);
    return [];
  }
}

// Inicializar mapa y dibujar zonas
async function initMap() {
  if (process.server || !mapContainer.value) return;

  L = await import("leaflet");
  import("leaflet/dist/leaflet.css");

  if (!mapInstance.value) {
    mapInstance.value = L.map(mapContainer.value).setView([-33.4489, -70.6693], 12); // Santiago por defecto
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap'
    }).addTo(mapInstance.value);
    
    layerGroup.value = L.featureGroup().addTo(mapInstance.value);
  }

  drawPolygons();
}

function drawPolygons() {
  if (!layerGroup.value || !zonas.value) return;

  layerGroup.value.clearLayers(); // Limpiar dibujos anteriores

  zonas.value.forEach(zona => {
    const latLngs = parseWKT(zona.area);
    if (latLngs.length > 0) {
      const polygon = L.polygon(latLngs, {
        color: '#ff4444',
        fillColor: '#ff4444',
        fillOpacity: 0.3
      }).bindPopup(`<b>${zona.nombre}</b><br>ID: ${zona.id}`);
      
      layerGroup.value.addLayer(polygon);
    }
  });

  // Ajustar el zoom para ver todos los polígonos si existen
  if (zonas.value.length > 0 && layerGroup.value.getLayers().length > 0) {
    mapInstance.value.fitBounds(layerGroup.value.getBounds());
  }
}

onMounted(() => {
  initMap();
});

// Volver a dibujar si los datos cambian (después de crear o eliminar)
watch(zonas, () => {
  drawPolygons();
}, { deep: true });

async function onCreated() {
  showModal.value = false;
  await refresh();
}

async function deleteZona(zona: any) {
  const id = zona?.id;
  if (!id) return;
  const ok = confirm(`¿Seguro que quieres eliminar la zona "${zona.nombre}"?`);
  if (!ok) return;

  deletingId.value = id;
  try {
    await $fetch(`/api/zonas/${id}`, { method: "DELETE" });
    await refresh();
  } catch (e: any) {
    alert(e?.data?.message || e?.message || "Error al eliminar zona");
  } finally {
    deletingId.value = null;
  }
}
</script>

<template>
  <main class="container">
    <div class="stack">
      <div class="header-section">
        <h2 style="margin: 0">Zonas Prohibidas</h2>
        <div style="display: flex; gap: 0.5rem">
          <button class="contrast btn-compact" type="button" @click="showModal = true">
            Crear Zona Prohibida
          </button>
        </div>
      </div>
    </div>

    <div ref="mapContainer" class="map-view"></div>

    <ZonaProhibidaCreate v-model:show="showModal" @created="onCreated" />

    <article v-if="status === 'pending'" aria-busy="true" />

    <article v-else-if="error" class="error">
      {{ error.statusMessage || error.message }}
    </article>

    <section v-else class="zonas-grid">
      <article v-for="zona in zonas" :key="zona.id" class="card zona-card">
        <div class="card-header">
          <h3 style="margin: 0">Zona #{{ zona.id }}</h3>
        </div>
        <div class="card-content">
          <p class="info-row">
            <strong>Nombre:</strong> {{ zona.nombre }}
          </p>
        </div>
        <div class="card-actions">
          <button
            class="contrast"
            :disabled="deletingId === zona.id"
            :aria-busy="deletingId === zona.id"
            @click="deleteZona(zona)"
          >
            {{ deletingId === zona.id ? "Eliminando..." : "Eliminar" }}
          </button>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>

.header-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}

.map-view {
  height: 400px;
  width: 100%;
  border-radius: 8px;
  margin-bottom: 2rem;
  z-index: 1; /* Asegura que los popups queden bajo modales si los hay */
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.zonas-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}

@media (max-width: 768px) {
  .zonas-grid {
    grid-template-columns: 1fr;
  }
}

.zona-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.card-header {
  padding: 1rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.02);
}

.card-content {
  padding: 1rem;
  flex: 1;
  overflow-y: auto;
}

.info-row {
  margin: 0.5rem 0;
  font-size: 0.95rem;
  line-height: 1.4;
}

.info-row strong {
  display: inline-block;
  min-width: 100px;
  color: rgba(255, 255, 255, 0.8);
}

.card-actions {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-top: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.02);
}

.card-actions button {
  flex: 1;
  padding: 0.5rem 0.75rem;
  font-size: 0.9rem;
}

.btn-compact {
  padding: 0.45rem 0.6rem;
  font-size: 0.85rem;
  white-space: nowrap;
  height: auto;
  display: inline-flex;
  align-items: center;
}

.zonas-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}
</style>
