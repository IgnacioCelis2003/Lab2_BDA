<template>
  <div class="polygon-picker-container">
    <div class="controls">
      <p class="instructions">Haz click en el mapa para agregar puntos. Mínimo 3 puntos para formar un polígono.</p>
      <div class="button-group">
        <button v-if="points.length > 0" class="btn-secondary" @click="deshacer">
          ↶ Deshacer
        </button>
        <button v-if="points.length >= 3" class="btn-success" @click="completarPoligono">
          ✓ Completar Polígono
        </button>
        <button v-if="points.length > 0" class="btn-danger" @click="limpiar">
          🗑 Limpiar
        </button>
      </div>
      <div v-if="points.length > 0" class="points-info">
        Puntos agregados: <strong>{{ points.length }}</strong>
      </div>
    </div>
    <div ref="mapRef" class="polygon-map"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

const emit = defineEmits<{
  (e: 'polygon-selected', v: string): void;
}>();

const mapRef = ref<HTMLDivElement | null>(null);
let map: any = null;
let L: any = null;
let resizeObserver: ResizeObserver | null = null;
let windowResizeHandler: (() => void) | null = null;

const points = ref<[number, number][]>([]);
let markers: any[] = [];
let polygon: any = null;

const round6 = (n: number) => Math.round(n * 1e6) / 1e6;

const agregarPunto = (lat: number, lon: number) => {
  const latR = round6(lat);
  const lonR = round6(lon);
  points.value.push([latR, lonR]);
  
  // Agregar marcador
  const marker = L.marker([latR, lonR], { 
    draggable: false,
    title: `Punto ${points.value.length}`
  }).addTo(map!);

  // Agregar número al marcador
  const numberIcon = L.divIcon({
    className: 'point-number',
    html: `<div>${points.value.length}</div>`
  });
  marker.setIcon(numberIcon);
  markers.push(marker);

  // Actualizar polígono si hay al menos 3 puntos
  if (points.value.length >= 3) {
    actualizarPoligono();
  }
};

const actualizarPoligono = () => {
  if (points.value.length < 3) return;

  if (polygon) {
    map?.removeLayer(polygon);
  }

  // Crear polígono
  const latLngs = points.value.map(([lat, lon]) => [lat, lon] as [number, number]);
  polygon = L.polygon(latLngs, {
    color: '#0066cc',
    fillColor: '#0066cc',
    fillOpacity: 0.2,
    weight: 2
  }).addTo(map!);

  map?.fitBounds(polygon.getBounds());
  // ensure proper rendering
  setTimeout(() => map?.invalidateSize?.(), 100);
};

const deshacer = () => {
  if (points.value.length === 0) return;
  
  points.value.pop();
  
  if (markers.length > 0) {
    const lastMarker = markers.pop();
    if (lastMarker) {
      map?.removeLayer(lastMarker);
    }
  }

  if (polygon && points.value.length < 3) {
    map?.removeLayer(polygon);
    polygon = null;
  } else if (points.value.length >= 3) {
    actualizarPoligono();
  }
};

const limpiar = () => {
  points.value = [];
  markers.forEach(m => map?.removeLayer(m));
  markers = [];
  if (polygon) {
    map?.removeLayer(polygon);
    polygon = null;
  }
};

const completarPoligono = () => {
  if (points.value.length < 3) return;

  // Cerrar el polígono - agregar el primer punto al final
  const coordenadas = points.value.map(([lat, lon]) => `${round6(lon)} ${round6(lat)}`);
  // Agregar el primer punto al final para cerrar el anillo
  coordenadas.push(coordenadas[0]);
  
  const wkt = `POLYGON((${coordenadas.join(', ')}))`;
  
  console.log('[PolygonPicker] WKT generado:', wkt);
  
  emit('polygon-selected', wkt);
  limpiar();
};

onMounted(async () => {
  if (!mapRef.value) return;

  // Importar Leaflet solo en el cliente
  L = (await import('leaflet')).default;
  const { Icon } = await import('leaflet');
  await import('leaflet/dist/leaflet.css');

  // Fix marker icon URLs
  delete (Icon.Default.prototype as any)._getIconUrl;
  Icon.Default.mergeOptions({
    iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
    iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).href,
    shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).href,
  });

  const defaultLat = -33.4372;
  const defaultLon = -70.6700;

  map = L.map(mapRef.value, { zoomControl: true }).setView([defaultLat, defaultLon], 12);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(map);

  // fix: invalidate size after rendering and when container resizes
  setTimeout(() => map?.invalidateSize?.(), 200);

  windowResizeHandler = () => map?.invalidateSize?.();
  window.addEventListener('resize', windowResizeHandler);

  // Observe container size changes and invalidate map
  try {
    resizeObserver = new ResizeObserver(() => {
      map?.invalidateSize?.();
    });
    if (mapRef.value) resizeObserver.observe(mapRef.value);
  } catch (e) {
    // ResizeObserver may not be available in some environments - ignore
  }

  map.on('click', (e: any) => {
    const ll = e.latlng;
    agregarPunto(ll.lat, ll.lng);
  });
});

onUnmounted(() => {
  if (resizeObserver && mapRef.value) {
    try { resizeObserver.unobserve(mapRef.value); } catch (e) {}
    resizeObserver = null;
  }
  if (windowResizeHandler) {
    window.removeEventListener('resize', windowResizeHandler);
    windowResizeHandler = null;
  }
  if (map) {
    try { map.remove(); } catch (e) {}
    map = null;
  }
});
</script>

<style scoped>
.polygon-picker-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
  background: rgba(50, 50, 70, 0.6);
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #505070;
}

.instructions {
  margin: 0 0 10px 0;
  color: #c0c0c0;
  font-size: 14px;
  font-weight: 500;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

button {
  padding: 8px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  transition: all 0.3s ease;
}

.btn-secondary {
  background: #505070;
  color: #c0c0c0;
}

.btn-secondary:hover {
  background: #606080;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover {
  background: #218838;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

.points-info {
  padding: 10px;
  background: rgba(107, 140, 255, 0.15);
  border-left: 4px solid #6b8cff;
  color: #a8c5ff;
  font-size: 14px;
  border-radius: 4px;
}

.polygon-map {
  width: 100%;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #505070;
}

:deep(.point-number) {
  background: #6b8cff !important;
  color: white !important;
  border-radius: 50% !important;
  width: 32px !important;
  height: 32px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-weight: bold !important;
  font-size: 14px !important;
  border: 2px solid white !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2) !important;
}
</style>
