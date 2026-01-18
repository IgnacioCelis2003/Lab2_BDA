<script setup lang="ts">
import { ref, onMounted } from "vue";
// Importamos el componente del mapa
import PtoInteresMap from "~/components/Lab2/PtoInteresMap.vue";

// --- INTERFACES ---
interface Mision {
  idMision: number;
  descripcion?: string;
  estado: string;
}

interface PuntoInteres {
  poi_id: number;
  nombre: string;
}

interface MisionEstadisticaDTO {
  misionId: number;
  distanciaTotalMetros: number;
  unidad: string;
}

interface ProximidadDTO {
  misionId: number;
  poiId: number;
  distanciaMinima3DMetros: number;
}

// 1. DEFINIR LA VARIABLE QUE FALTABA
// --- ESTADOS REACTIVOS ---
const misiones = ref<Mision[]>([]);
const puntosInteres = ref<PuntoInteres[]>([]);

const selectedMisionId = ref<string>("");
const selectedPoiId = ref<string>("");

const resultadoDistanciaVuelo = ref<string | null>(null);
const resultadoProximidad = ref<string | null>(null);

const loading = ref(false);
const loadingProximidad = ref(false);
const errorMsg = ref<string | null>(null);

const mapaRef = ref<any>(null);

// --- 1. CARGA DE DATOS INICIALES ---
const cargarDatos = async () => {
  try {
    // 1. Cargar Misiones (Ruta corregida a 'all')
    const resMisiones = await $fetch<Mision[]>("/api/misiones/all");
    misiones.value = resMisiones || [];

    // 2. Cargar Puntos de Interés (Ruta corregida a 'all')
    const resPoi = await $fetch<PuntoInteres[]>("/api/puntosInteres/all");
    puntosInteres.value = resPoi || [];
  } catch (error) {
    console.error("Error cargando listas:", error);
    errorMsg.value = "Error conectando con el servidor.";
  }
};

// --- 2. CÁLCULO: Longitud Real de Vuelo ---
async function calcularLongitudVuelo() {
  if (!selectedMisionId.value) return;

  try {
    // 1. Activar estado de carga (bloquea el botón)
    loading.value = true;
    resultadoDistanciaVuelo.value = null; // Limpiar resultado anterior

    // 2. Llamada al endpoint (usando tu archivo server existente)
    const url = `/api/misiones/${selectedMisionId.value}/distancia`;

    const response = await $fetch<MisionEstadisticaDTO>(url);
    console.log("Datos recibidos:", response);

    // 3. Procesar y Asignar a la variable CORRECTA
    const metros = response.distanciaTotalMetros;

    if (typeof metros === "number") {
      const km = metros / 1000;
      // Aquí asignamos el valor a la variable que tu HTML está esperando
      resultadoDistanciaVuelo.value = `${km.toFixed(2)} km`;
    } else {
      resultadoDistanciaVuelo.value = "Datos no válidos";
    }
  } catch (error: any) {
    console.error("Error:", error);
    alert(error.statusMessage || "Error al calcular la distancia");
    resultadoDistanciaVuelo.value = "Error";
  } finally {
    // 4. Desactivar carga siempre (haya éxito o error)
    loading.value = false;
  }
}
// --- 3. CÁLCULO: Proximidad a POI ---
async function calcularProximidad() {
  if (!selectedMisionId.value || !selectedPoiId.value) {
    alert("Debes seleccionar Misión y POI.");
    return;
  }

  try {
    loadingProximidad.value = true;
    resultadoProximidad.value = null;

    // Llamamos a nuestro proxy.
    // Aunque el backend pide ruta, al proxy se lo pasamos como parámetro
    // para no complicar el enrutador de Nuxt.
    const url = `/api/misiones/${selectedMisionId.value}/proximidad`;

    // TypeScript ahora sabe que recibirá un ProximidadDTO
    const response = await $fetch<ProximidadDTO>(url, {
      params: { poiId: selectedPoiId.value },
    });

    console.log("Proximidad recibida:", response);

    // 3. LEER EL CAMPO CORRECTO ("distancia")
    const metros = response.distanciaMinima3DMetros;

    if (typeof metros === "number") {
      // Metros si es cerca, KM si es lejos
      if (metros < 1000) {
        resultadoProximidad.value = `${metros.toFixed(2)} metros`;
      } else {
        resultadoProximidad.value = `${(metros / 1000).toFixed(2)} km`;
      }
    } else {
      resultadoProximidad.value = "Datos inválidos";
    }
  } catch (error: any) {
    console.error("Error:", error);
    alert(error.statusMessage || "Error al calcular proximidad");
    resultadoProximidad.value = "Error";
  } finally {
    loadingProximidad.value = false;
  }
}

async function verRutaEnMapa() {
  if (!selectedMisionId.value) {
    alert("Selecciona una misión primero.");
    return;
  }
  // Llamamos a la función que creamos en el hijo
  if (mapaRef.value) {
    mapaRef.value.dibujarRutaVisual(selectedMisionId.value);
  }
}

// Al montar el componente, cargamos los selects
onMounted(() => {
  cargarDatos();
});
</script>

<template>
  <div class="page-container">
    <header class="header">
      <h1>Análisis Espacial de Vuelos</h1>
      <p class="subtitle">
        Calcula distancias reales, verifica cercanía a zonas de interés y
        visualiza trayectorias.
      </p>
    </header>

    <div v-if="errorMsg" class="alert-error">
      {{ errorMsg }}
    </div>

    <div class="control-panel">
      <div class="form-group">
        <label for="misionSelect">Seleccionar Misión:</label>
        <select
          id="misionSelect"
          v-model="selectedMisionId"
          class="select-input"
        >
          <option disabled value="">-- Elige una Misión --</option>
          <option v-for="m in misiones" :key="m.idMision" :value="m.idMision">
            ID {{ m.idMision }} - {{ m.estado }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label for="poiSelect">Seleccionar Punto de Interés:</label>
        <select id="poiSelect" v-model="selectedPoiId" class="select-input">
          <option disabled value="">-- Elige un POI --</option>
          <option v-for="p in puntosInteres" :key="p.poi_id" :value="p.poi_id">
            {{ p.nombre }}
          </option>
        </select>
      </div>
    </div>

    <div class="cards-container">
      <div class="card">
        <div class="card-icon">📏</div>
        <h3>Longitud Real de Vuelo</h3>
        <p>Calcula la distancia total recorrida reconstruyendo la ruta 3D.</p>

        <button
          @click="calcularLongitudVuelo"
          :disabled="loading || !selectedMisionId"
          class="btn btn-primary"
        >
          {{ loading ? "Calculando..." : "Calcular Distancia" }}
        </button>

        <div v-if="resultadoDistanciaVuelo" class="result-box">
          <span class="label">Total Recorrido:</span>
          <span class="value">{{ resultadoDistanciaVuelo }}</span>
        </div>
      </div>

      <div class="card">
        <div class="card-icon">🎯</div>
        <h3>Proximidad a POI</h3>
        <p>
          Verifica la distancia mínima a la que pasó el dron del punto
          seleccionado.
        </p>

        <button
          @click="calcularProximidad"
          :disabled="loadingProximidad || !selectedMisionId || !selectedPoiId"
          class="btn btn-warning"
        >
          {{ loadingProximidad ? "Verificando..." : "Verificar Cercanía" }}
        </button>

        <div v-if="resultadoProximidad" class="result-box">
          <span class="label">Distancia Mínima:</span>
          <span class="value">{{ resultadoProximidad }}</span>
        </div>
      </div>

      <div class="card">
        <div class="card-icon">🗺️</div>
        <h3>Trayectoria de Vuelo</h3>
        <p>Proyectar la telemetría real sobre el mapa (Estilo HUD).</p>

        <button
          class="btn btn-info"
          @click="verRutaEnMapa"
          :disabled="!selectedMisionId"
          style="background-color: #0ea5e9; border: none; color: white"
        >
          👁️ Ver Ruta en Mapa
        </button>
      </div>
    </div>

    <div class="map-section">
      <h2>Visualización Geográfica</h2>
      <div class="map-wrapper">
        <ClientOnly fallback-tag="div" fallback="Cargando mapa...">
          <PtoInteresMap
            ref="mapaRef"
            :misionId="selectedMisionId"
            :poiId="selectedPoiId"
          />
        </ClientOnly>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* --- CONFIGURACIÓN GENERAL --- */
.page-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  color: #e2e8f0; /* Texto claro (casi blanco) */
}

.header {
  text-align: center;
  margin-bottom: 40px;
}
.header h1 {
  color: #f8fafc; /* Blanco brillante */
  margin-bottom: 10px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}
.subtitle {
  color: #94a3b8; /* Gris azulado claro */
  font-size: 1.1rem;
}

/* --- ALERTAS --- */
.alert-error {
  background-color: rgba(220, 38, 38, 0.2); /* Rojo oscuro transparente */
  color: #fca5a5; /* Texto rojo claro */
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  text-align: center;
  border: 1px solid rgba(220, 38, 38, 0.5);
}

/* --- PANEL DE CONTROLES (Oscuro y Opaco) --- */
.control-panel {
  display: flex;
  gap: 20px;
  /* Fondo Azul Oscuro Semitransparente (Glassmorphism) */
  background-color: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(10px);
  padding: 25px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1); /* Borde sutil */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.form-group {
  flex: 1;
  min-width: 250px;
  display: flex;
  flex-direction: column;
}

label {
  font-weight: 600;
  margin-bottom: 8px;
  color: #cbd5e1; /* Gris claro */
}

/* INPUTS SELECT (Estilo Integrado) */
.select-input {
  padding: 12px;
  border-radius: 8px;
  font-size: 1rem;
  /* Fondo oscuro para los inputs */
  background-color: #0f172a;
  color: #ffffff;
  border: 1px solid #334155; /* Borde azul grisáceo */
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
  appearance: none; /* Quita el estilo por defecto del navegador */

  /* Flechita personalizada (opcional, para asegurar visibilidad) */
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  background-size: 1em;
}

.select-input:focus {
  outline: none;
  border-color: #60a5fa; /* Azul brillante al hacer click */
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.2);
}

/* --- CARDS (Tarjetas) --- */
.cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  margin-bottom: 40px;
}

.card {
  /* Fondo igual al panel de control */
  background-color: rgba(30, 41, 59, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transition:
    transform 0.2s,
    background-color 0.2s;
}
.card:hover {
  transform: translateY(-5px);
  background-color: rgba(30, 41, 59, 0.9);
}

.card-icon {
  font-size: 3rem;
  margin-bottom: 15px;
  filter: drop-shadow(0 0 10px rgba(255, 255, 255, 0.2));
}
.card h3 {
  margin-bottom: 10px;
  color: #f1f5f9;
}
.card p {
  color: #94a3b8;
  margin-bottom: 20px;
  font-size: 0.95rem;
}

/* --- BOTONES --- */
.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-size: 0.9rem;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(1);
}

.btn-primary {
  background-color: #2563eb; /* Azul intenso */
  color: white;
  box-shadow: 0 4px 6px rgba(37, 99, 235, 0.3);
}
.btn-primary:hover:not(:disabled) {
  background-color: #1d4ed8;
}

.btn-warning {
  background-color: #d97706; /* Ambar oscuro para contrastar mejor */
  color: white;
  box-shadow: 0 4px 6px rgba(217, 119, 6, 0.3);
}
.btn-warning:hover:not(:disabled) {
  background-color: #b45309;
}

/* --- RESULTADOS --- */
.result-box {
  margin-top: 20px;
  background-color: rgba(15, 23, 42, 0.6); /* Fondo muy oscuro */
  padding: 15px;
  border-radius: 8px;
  border-left: 5px solid #3b82f6;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.result-box .label {
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #60a5fa; /* Azul claro */
}
.result-box .value {
  font-size: 1.5rem;
  font-weight: bold;
  color: #ffffff;
  margin-top: 5px;
  text-shadow: 0 0 10px rgba(59, 130, 246, 0.5); /* Brillito neón sutil */
}

/* --- MAPA --- */
.map-section h2 {
  color: #f1f5f9;
  margin-bottom: 15px;
}
.map-wrapper {
  margin-top: 20px;
  border: 1px solid #475569; /* Borde oscuro */
  border-radius: 12px;
  padding: 10px;
  background-color: #1e293b; /* Fondo del marco del mapa */
  min-height: 200px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}
</style>
