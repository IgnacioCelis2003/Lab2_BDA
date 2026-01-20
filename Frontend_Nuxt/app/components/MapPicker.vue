<template>
  <div ref="mapRef" style="height:300px;border-radius:8px;overflow:hidden;border:1px solid rgba(0,0,0,0.06);"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import L, { Icon } from 'leaflet';
import 'leaflet/dist/leaflet.css';

const props = defineProps<{ 
  lat?: number; 
  lon?: number; 
  altitudInicio?: number | null;
  altitudFin?: number | null;
  mode?: 'single' | 'route'; 
  rutaWKT?: string | null 
}>();
const emit = defineEmits<{
  (e: 'update:lat', v: number): void;
  (e: 'update:lon', v: number): void;
  (e: 'update:altitudInicio', v: number | null): void;
  (e: 'update:altitudFin', v: number | null): void;
  (e: 'update:rutaWKT', v: string | null): void;
}>();

// Fetch elevation para un punto de ruta (inicio o fin)
async function fetchRouteElevation(lon: number, lat: number, point: 'inicio' | 'fin') {
  try {
    const data = await $fetch<{ elevation: number }>(`/api/elevation/${lon}/${lat}`);
    if (point === 'inicio') {
      emit('update:altitudInicio', Math.round(data.elevation));
    } else {
      emit('update:altitudFin', Math.round(data.elevation));
    }
  } catch (e) {
    console.error(`Error obteniendo elevación punto ${point}:`, e);
  }
}

const mapRef = ref<HTMLDivElement | null>(null);
const zonas = ref<{ id: number; nombre: string; area: string }[]>([]);
let map: L.Map | null = null;
let marker: L.Marker | null = null;
let markers: L.Marker[] = [];
let polyline: L.Polyline | null = null;
let zonasLayer: L.FeatureGroup | null = null;

const round6 = (n: number) => Math.round(n * 1e6) / 1e6;

// Fix marker icon URLs for bundlers
delete (Icon.Default.prototype as any)._getIconUrl;
Icon.Default.mergeOptions({
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).href,
  shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).href,
});

// Parse WKT POLYGON((lng lat, ...)) -> [[lat, lng], ...]
function parseWKT(wkt: string) {
  try {
    const coordsString = wkt.replace('POLYGON((', '').replace('))', '');
    const pairs = coordsString.split(',');
    return pairs.map(pair => {
      const [lng, lat] = pair.trim().split(' ').map(Number);
      return [lat, lng] as [number, number];
    });
  } catch (e) {
    console.error('Error parseando WKT:', e);
    return [];
  }
}

function drawZonas() {
  if (!zonasLayer || !zonas.value) return;
  zonasLayer.clearLayers();

  zonas.value.forEach(zona => {
    const latLngs = parseWKT(zona.area);
    if (latLngs.length > 0) {
      const polygon = L.polygon(latLngs, {
        color: '#ff4444',
        fillColor: '#ff4444',
        fillOpacity: 0.25
      }).bindPopup(`<b>${zona.nombre}</b><br>ID: ${zona.id}`);
      zonasLayer!.addLayer(polygon);
    }
  });
}

async function fetchZonas() {
  try {
    const data = await $fetch<{ id: number; nombre: string; area: string }[]>('/api/zonas/all');
    zonas.value = data || [];
    drawZonas();
  } catch (e) {
    console.error('Error trayendo zonas:', e);
  }
}

onMounted(() => {
  if (!mapRef.value) return;

  // Estación Central, Santiago de Chile
  const defaultLat = -33.4372;
  const defaultLon = -70.6700;

  const initialLat = typeof props.lat === 'number' ? props.lat : defaultLat;
  const initialLon = typeof props.lon === 'number' ? props.lon : defaultLon;

  map = L.map(mapRef.value, { zoomControl: true }).setView([initialLat, initialLon], 12);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(map);

  // Layer para zonas prohibidas
  zonasLayer = L.featureGroup().addTo(map);
  fetchZonas();

  // initialize marker(s) depending on mode
  if (props.mode === 'route') {
    markers = [];
    polyline = L.polyline([], { color: 'blue' }).addTo(map);
  } else {
    marker = L.marker([initialLat, initialLon], { draggable: true }).addTo(map);

    marker.on('dragend', (e) => {
      const p = (e.target as L.Marker).getLatLng();
      const lat = round6(p.lat);
      const lon = round6(p.lng);
      marker!.setLatLng([lat, lon]);
      emit('update:lat', lat);
      emit('update:lon', lon);
    });
  }

  map.on('click', (e: L.LeafletMouseEvent) => {
    const ll = e.latlng;
    const lat = round6(ll.lat);
    const lon = round6(ll.lng);

    if (props.mode === 'route') {
      // add or replace markers (keep up to 2)
      if (markers.length < 2) {
        const m = L.marker([lat, lon], { draggable: true }).addTo(map!);
        m.on('dragend', onRouteMarkerDrag);
        markers.push(m);
        // Fetch elevation para el punto recién agregado
        const pointType = markers.length === 1 ? 'inicio' : 'fin';
        fetchRouteElevation(lon, lat, pointType);
      } else {
        // replace second marker
        const m = markers[1];
        m.setLatLng([lat, lon]);
        // Actualizar elevación del punto final
        fetchRouteElevation(lon, lat, 'fin');
      }
      updatePolylineAndEmit();
      // emit last clicked as lat/lon as well
      emit('update:lat', lat);
      emit('update:lon', lon);
    } else {
      if (!marker) {
        marker = L.marker([lat, lon], { draggable: true }).addTo(map!);
        marker.on('dragend', (ev) => {
          const p = (ev.target as L.Marker).getLatLng();
          const rlat = round6(p.lat);
          const rlon = round6(p.lng);
          marker!.setLatLng([rlat, rlon]);
          emit('update:lat', rlat);
          emit('update:lon', rlon);
        });
      } else {
        marker.setLatLng([lat, lon]);
      }
      emit('update:lat', lat);
      emit('update:lon', lon);
    }
  });

  // double-click to clear route markers
  map.on('dblclick', () => {
    if (props.mode === 'route') {
      clearRoute();
    }
  });
});

watch(() => props.lat, (v) => {
  if (v == null || !marker) return;
  const lon = props.lon ?? 0;
  const latR = round6(v);
  const lonR = round6(lon);
  marker.setLatLng([latR, lonR]);
  map?.setView([latR, lonR], map.getZoom());
});

watch(() => props.lon, (v) => {
  if (v == null || !marker) return;
  const lat = props.lat ?? 0;
  const latR = round6(lat);
  const lonR = round6(v);
  marker.setLatLng([latR, lonR]);
  map?.setView([latR, lonR], map.getZoom());
});

function onRouteMarkerDrag(ev: L.DragEndEvent) {
  const p = (ev.target as L.Marker).getLatLng();
  const rlat = round6(p.lat);
  const rlon = round6(p.lng);
  (ev.target as L.Marker).setLatLng([rlat, rlon]);
  updatePolylineAndEmit();
  
  // Determinar si es el marcador de inicio o fin y actualizar su elevación
  const markerIndex = markers.indexOf(ev.target as L.Marker);
  if (markerIndex === 0) {
    fetchRouteElevation(rlon, rlat, 'inicio');
  } else if (markerIndex === 1) {
    fetchRouteElevation(rlon, rlat, 'fin');
  }
}

function updatePolylineAndEmit() {
  const latlngs = markers.map(m => m.getLatLng());
  if (polyline) {
    polyline.setLatLngs(latlngs);
  }
  if (latlngs.length >= 2) {
    // build WKT: LINESTRING(lon lat, lon lat)
    const coords = latlngs.map(p => `${round6(p.lng)} ${round6(p.lat)}`).join(', ');
    const wkt = `LINESTRING(${coords})`;
    emit('update:rutaWKT', wkt);
  } else {
    emit('update:rutaWKT', null);
  }
}

function clearRoute() {
  markers.forEach(m => map && map.removeLayer(m));
  markers = [];
  if (polyline) polyline.setLatLngs([]);
  emit('update:rutaWKT', null);
  emit('update:altitudInicio', null);
  emit('update:altitudFin', null);
}
</script>

<style scoped>
/* Ensure leaflet map fills container */
div[ref="mapRef"] {
  width: 100%;
}
</style>
