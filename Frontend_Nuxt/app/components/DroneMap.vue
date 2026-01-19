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

async function fetchVelocidad(idMision) {
    try {
        // 1. Hacemos la petición (Recibimos un ARRAY de datos históricos)
        const data = await $fetch(`/api/telemetria/mision/velocidad-media/${idMision}`);
        
        // 2. Validación de seguridad
        if (!data || !Array.isArray(data) || data.length === 0) {
            console.warn("No hay datos de velocidad para misión", idMision);
            return null;
        }

        // 3. Tomamos el ultimo dato (el más reciente en el tiempo)
        const datoActual = data[data.length - 1];

        return datoActual; 
    } catch (e) {
        console.error("Error trayendo velocidad", e);
        return null;
    }
}

// Obtener drones y actualizar marcadores
async function fetchDrones() {
  try {
    // Usar $fetch en cliente
    const data = await $fetch('/api/telemetria/monitoreo');
    drones.value = data || [];

    drones.value.forEach(drone => {
      const icon = getDroneIcon(drone.nivelBateriaPorcentaje);

      // Contenido base del Popup
      const basePopupContent = `
         <div style="min-width: 150px">
            <h4 style="margin:0 0 5px 0; color: #333;">Misión #${drone.idMision}</h4>
            <div>🔋 Batería: <b>${drone.nivelBateriaPorcentaje}%</b></div>
            <div style="font-size: 0.8em; color: #666; margin-bottom: 5px">
                Actualizado: ${new Date(drone.timestamp).toLocaleTimeString()}
            </div>
            <hr style="margin: 5px 0; border: 0; border-top: 1px solid #eee;">
            <div id="speed-content-${drone.idMision}" style="font-size: 0.9em;">
                <i>Haga clic para ver telemetría...</i>
            </div>
         </div>
      `;

      let marker;

      if (markers.value[drone.idMision]) {
        // Actualizar posición y popup
        marker = markers.value[drone.idMision];
        marker.setLatLng([drone.latitud, drone.longitud]);
        marker.setIcon(icon);
        if (!marker.isPopupOpen()) {
          marker.bindPopup(basePopupContent);
        }
      } else {
        // Si es nuevo, lo creamos
        marker = L.marker([drone.latitud, drone.longitud], { icon }).addTo(map.value);
        marker.bindPopup(basePopupContent);
        markers.value[drone.idMision] = marker;
        
        // Evento al hacer clic en el marcador 
        marker.on('click', async () => {
          // 1. Buscamos el contenedor del popup (si está abierto)
          const container = document.getElementById(`speed-content-${drone.idMision}`);
          if (container) {
            container.innerHTML = 'Calculando velocidad media... ⏳';
          }

          // 2. Llamamos al Backend
          const velocidadData = await fetchVelocidad(drone.idMision);

          // 3. Actualizamos el HTML del popup con el resultado
          const finalContainer = document.getElementById(`speed-content-${drone.idMision}`);
          if (finalContainer) {
            if (velocidadData) {
              finalContainer.innerHTML = `
                <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 5px;">
                  <div>🚀 Velocidad media:</div>
                  <div style="font-weight:bold; color: #2563eb">
                    ${(velocidadData.velocidadCalculada || 0).toFixed(2)} km/h
                  </div>
                    
                  <div>📏 Distancia recorrida:</div>
                  <div>${(velocidadData.distanciaRecorrida || 0).toFixed(1)} m</div>
                    
                  <div>⏱️ Tiempo transcurrido:</div>
                  <div>${(velocidadData.segundosTranscurridos || 0).toFixed(1)} s</div>
                </div>
            `;
            } else {
              finalContainer.innerHTML = '<span style="color:red">Error calculando datos</span>';
            }
          }
        });
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
  height: 600px;
  width: 100%;
  max-width: 90%;
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
