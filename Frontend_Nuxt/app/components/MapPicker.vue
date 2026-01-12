<template>
  <div ref="mapRef" style="height:300px;border-radius:8px;overflow:hidden;border:1px solid rgba(0,0,0,0.06);"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import L, { Icon } from 'leaflet';
import 'leaflet/dist/leaflet.css';

const props = defineProps<{ lat?: number; lon?: number; mode?: 'single' | 'route'; rutaWKT?: string | null }>();
const emit = defineEmits<{
  (e: 'update:lat', v: number): void;
  (e: 'update:lon', v: number): void;
  (e: 'update:rutaWKT', v: string | null): void;
}>();

const mapRef = ref<HTMLDivElement | null>(null);
let map: L.Map | null = null;
let marker: L.Marker | null = null;
let markers: L.Marker[] = [];
let polyline: L.Polyline | null = null;

const round6 = (n: number) => Math.round(n * 1e6) / 1e6;

// Fix marker icon URLs for bundlers
delete (Icon.Default.prototype as any)._getIconUrl;
Icon.Default.mergeOptions({
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).href,
  shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).href,
});

onMounted(() => {
  if (!mapRef.value) return;

  const initialLat = typeof props.lat === 'number' ? props.lat : 0;
  const initialLon = typeof props.lon === 'number' ? props.lon : 0;

  map = L.map(mapRef.value, { zoomControl: true }).setView([initialLat, initialLon], (initialLat || initialLon) ? 12 : 2);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(map);

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
      } else {
        // replace second marker
        const m = markers[1];
        m.setLatLng([lat, lon]);
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
}
</script>

<style scoped>
/* Ensure leaflet map fills container */
div[ref="mapRef"] {
  width: 100%;
}
</style>
