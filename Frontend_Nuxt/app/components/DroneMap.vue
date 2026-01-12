<template>
  <div class="map-container">
    <div id="map"></div>
    <button @click="fetchDrones" class="update-button">
      ↻ Actualizar
    </button>
    <div v-if="drones.length === 0" class="overlay">
      No hay drones activos
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const map = ref(null);
const markers = ref({});
const drones = ref([]);
let L; // Leaflet import dinámico

// Crear icono según nivel de batería
function getDroneIcon(bateria) {
  let color;
  if (bateria > 50) color = '#22c55e';
  else if (bateria > 20) color = '#f59e0b';
  else color = '#ef4444';

  return L.divIcon({
    className: 'custom-drone-icon',
    html: `<div style="
      width: 100%;
      height: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
      font-size: 30px; 
      filter: drop-shadow(0 2px 2px rgba(0,0,0,0.5));
    ">
      <span style="color: ${color}; line-height: 1;">🚁</span>
    </div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15], 
    popupAnchor: [0, -15]
  });
}

// Obtener drones y actualizar marcadores
async function fetchDrones() {
  try {
    // Usar $fetch en cliente
    const data = await $fetch('/api/telemetria/monitoreo');
    drones.value = data || [];

    drones.value.forEach(drone => {
      const icon = getDroneIcon(drone.nivelBateriaPorcentaje);

      if (markers.value[drone.idMision]) {
        // Actualizar posición y popup
        markers.value[drone.idMision].setLatLng([drone.latitud, drone.longitud]);
        markers.value[drone.idMision].setIcon(icon);
        markers.value[drone.idMision].bindPopup(`
          <b>Misión #${drone.idMision}</b><br>
          Batería: ${drone.nivelBateriaPorcentaje}%<br>
          Última actualización: ${new Date(drone.timestamp).toLocaleString()}
        `);
      } else {
        // Crear nuevo marcador
        const marker = L.marker([drone.latitud, drone.longitud], { icon }).addTo(map.value);
        marker.bindPopup(`
          <b>Misión #${drone.idMision}</b><br>
          Batería: ${drone.nivelBateriaPorcentaje}%<br>
          Última actualización: ${new Date(drone.timestamp).toLocaleString()}
        `);
        markers.value[drone.idMision] = marker;
      }
    });
  } catch (error) {
    console.error('Error al obtener drones:', error);
  }
}

onMounted(async () => {
  // Importar Leaflet solo en cliente
  L = await import('leaflet');

  // Inicializar mapa
  map.value = L.map('map').setView([-33.43, -70.64], 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map.value);

  // Primer fetch y actualización periódica
  await fetchDrones();
  //setInterval(fetchDrones, 5000);
});
</script>

<style scoped>
.map-container {
  position: relative;
  height: 500px;
  width: 100%;
}
#map {
  height: 100%;
  width: 100%;
}

.update-button {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 400;
  padding: 8px 16px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.update-button:hover {
  background-color: #2563eb;
}

.update-button:active {
  background-color: #1d4ed8;
}

.overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(255, 255, 255, 0.85);
  padding: 1rem 2rem;
  border-radius: 8px;
  font-weight: bold;
  color: #333;
  text-align: center;
  pointer-events: none;
}

.custom-drone-icon {
  background: transparent !important;
  border: none !important;
  width: auto !important;
  height: auto !important;
}

.custom-drone-icon div {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}
</style>
