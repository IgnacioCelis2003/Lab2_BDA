<script setup lang="ts">
import { ref, onMounted } from "vue";
import PtoInteresMap from "~/components/Lab2/PtoInteresMap.vue"; // Descomenta si lo necesitas aquí

// =========================================
// 1. INTERFACES
// =========================================

interface GeoJSON {
  type: "Point";
  coordinates: number[]; // [longitud, latitud]
}

interface PuntoInteresBD {
  poi_id: number;
  nombre: string;
  descripcion?: string;
  ubicacion: GeoJSON;
}

interface PuntoInteresUI extends PuntoInteresBD {
  latitud: number;
  longitud: number;
}

interface Mision {
  idMision: number;
  descripcion?: string;
  estado: string;
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

// =========================================
// 2. ESTADOS REACTIVOS
// =========================================

const misiones = ref<Mision[]>([]);
const puntosInteres = ref<PuntoInteresUI[]>([]);

const selectedMisionId = ref<string>("");
const selectedPoiId = ref<string>("");
const resultadoDistanciaVuelo = ref<string | null>(null);
const resultadoProximidad = ref<string | null>(null);

const loading = ref(false);
const loadingProximidad = ref(false);
const errorMsg = ref<string | null>(null);
const mapaRef = ref<any>(null);

// --- ESTADOS CRUD ---
const showAdminPanel = ref(false);
const loadingSave = ref(false);
const isEditing = ref(false);

// Variable para saber si estamos en modo "elegir ubicación" (Mapa 2)
const isPickingLocation = ref(false);

const poiForm = ref({
  id: null as number | null,
  nombre: "",
  descripcion: "",
  latitud: 0,
  longitud: 0,
});

// =========================================
// 3. CARGA DE DATOS
// =========================================

const cargarDatos = async () => {
  try {
    const resMisiones = await $fetch<Mision[]>("/api/misiones/all");
    misiones.value = resMisiones || [];

    const resPoi = await $fetch<PuntoInteresBD[]>("/api/puntosInteres/all");

    puntosInteres.value = (resPoi || []).map((p) => {
      let lat = 0;
      let lon = 0;
      if (p.ubicacion && Array.isArray(p.ubicacion.coordinates)) {
        lon = p.ubicacion.coordinates[0] ?? 0;
        lat = p.ubicacion.coordinates[1] ?? 0;
      }
      return { ...p, latitud: lat, longitud: lon };
    });
  } catch (error) {
    console.error("Error cargando listas:", error);
    errorMsg.value = "Error conectando con el servidor.";
  }
};

// =========================================
// 4. FUNCIONES DE MAPA (AQUÍ ESTÁN LAS DOS)
// =========================================

// --- A) PARA EL MAPA 1 (El que ya tenías) ---
// Esta función la dejamos intacta para no romper tu mapa actual.
const onMapaClick = (coords: { lat: number; lng: number }) => {
  // Si el panel está abierto, actualizamos coordenadas (comportamiento original)
  if (showAdminPanel.value) {
    poiForm.value.latitud = parseFloat(coords.lat.toFixed(6));
    poiForm.value.longitud = parseFloat(coords.lng.toFixed(6));
  }
};

// --- B) PARA EL MAPA 2 (El nuevo para elegir ubicación) ---
const activarSeleccionMapa = () => {
  showAdminPanel.value = false; // Ocultamos modal
  isPickingLocation.value = true; // Activamos bandera
  alert("Haz clic en el mapa de selección para ubicar el punto.");
};

const onCoordenadaCapturada = (coords: { lat: number; lng: number }) => {
  // Sin el "if (isPickingLocation)", así el clic siempre funciona
  poiForm.value.latitud = parseFloat(coords.lat.toFixed(6));
  poiForm.value.longitud = parseFloat(coords.lng.toFixed(6));

  // Opcional: Para ver en la consola que funcionó
  console.log(
    "Coordenada fijada:",
    poiForm.value.latitud,
    poiForm.value.longitud,
  );
};

// =========================================
// 5. CÁLCULOS
// =========================================

async function calcularLongitudVuelo() {
  if (!selectedMisionId.value) return;
  try {
    loading.value = true;
    resultadoDistanciaVuelo.value = null;
    const url = `/api/misiones/${selectedMisionId.value}/distancia`;
    const response = await $fetch<MisionEstadisticaDTO>(url);
    const metros = response.distanciaTotalMetros;

    if (typeof metros === "number") {
      resultadoDistanciaVuelo.value = `${(metros / 1000).toFixed(2)} km`;
    } else {
      resultadoDistanciaVuelo.value = "Datos no válidos";
    }
  } catch (error) {
    resultadoDistanciaVuelo.value = "Error";
  } finally {
    loading.value = false;
  }
}

async function calcularProximidad() {
  if (!selectedMisionId.value || !selectedPoiId.value) {
    alert("Debes seleccionar Misión y POI.");
    return;
  }
  try {
    loadingProximidad.value = true;
    resultadoProximidad.value = null;
    const url = `/api/misiones/${selectedMisionId.value}/proximidad`;
    const response = await $fetch<ProximidadDTO>(url, {
      params: { poiId: selectedPoiId.value },
    });
    const metros = response.distanciaMinima3DMetros;

    if (typeof metros === "number") {
      resultadoProximidad.value =
        metros < 1000
          ? `${metros.toFixed(2)} m`
          : `${(metros / 1000).toFixed(2)} km`;
    } else {
      resultadoProximidad.value = "Datos inválidos";
    }
  } catch (error) {
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
  if (mapaRef.value) {
    mapaRef.value.dibujarRutaVisual(selectedMisionId.value);
  }
}

// =========================================
// 6. GESTIÓN CRUD
// =========================================

const resetForm = () => {
  poiForm.value = {
    id: null,
    nombre: "",
    descripcion: "",
    latitud: 0,
    longitud: 0,
  };
  isEditing.value = false;
  isPickingLocation.value = false;
};

const editarPunto = (poi: PuntoInteresUI) => {
  poiForm.value = {
    id: poi.poi_id,
    nombre: poi.nombre,
    descripcion: poi.descripcion || "",
    latitud: poi.latitud,
    longitud: poi.longitud,
  };
  isEditing.value = true;
};

const guardarPunto = async () => {
  // 1. Validación completa (Nombre y Coordenadas)
  if (
    !poiForm.value.nombre ||
    !poiForm.value.latitud ||
    !poiForm.value.longitud
  ) {
    alert("Debes ingresar un nombre y seleccionar una ubicación en el mapa.");
    return;
  }

  try {
    loadingSave.value = true;

    // 2. CORRECCIÓN: Enviamos los datos "planos"
    // En vez de construir el GeoJSON aquí, mandamos lat y lng sueltos.
    // El backend se encargará de convertirlos a geometría.
    const payload = {
      nombre: poiForm.value.nombre,
      descripcion: poiForm.value.descripcion || "", // Evitar nulls
      latitud: Number(poiForm.value.latitud),
      longitud: Number(poiForm.value.longitud),
    };

    console.log("🚀 Enviando Payload:", payload);

    if (isEditing.value && poiForm.value.id) {
      // --- EDITAR (PUT) ---
      await $fetch(`/api/puntosInteres/${poiForm.value.id}`, {
        method: "PUT",
        body: payload,
      });
      alert("Punto actualizado con éxito.");
    } else {
      // --- CREAR (POST) ---
      await $fetch("/api/puntosInteres/crear", {
        method: "POST",
        body: payload,
      });
      alert("Punto creado con éxito.");
    }

    // 3. Refrescar la UI
    // Importante: Si tienes una función para recargar la lista del servidor, llámala aquí.
    // await fetchPuntosDeInteres();

    // Si no tienes esa función a mano, recarga la página temporalmente:
    window.location.reload();
  } catch (error) {
    console.error("❌ Error en la petición:", error);
    alert(
      "Error del servidor (500). Revisa que la base de datos esté conectada.",
    );
  } finally {
    loadingSave.value = false;
    showAdminPanel.value = false; // Cerramos el modal
    resetForm();
  }
};

const eliminarPunto = async (id: number) => {
  if (!confirm("¿Eliminar este punto?")) return;
  try {
    loadingSave.value = true;
    await $fetch(`/api/puntosInteres/${id}`, { method: "DELETE" });
    await cargarDatos();
    if (selectedPoiId.value == id.toString()) selectedPoiId.value = "";
  } catch (error) {
    alert("No se pudo eliminar.");
  } finally {
    loadingSave.value = false;
  }
};

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

    <div v-if="errorMsg" class="alert-error">{{ errorMsg }}</div>

    <div class="control-panel">
      <div class="panel-inputs">
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
            <option
              v-for="p in puntosInteres"
              :key="p.poi_id"
              :value="p.poi_id"
            >
              {{ p.nombre }}
            </option>
          </select>
        </div>
      </div>

      <div class="panel-actions">
        <button class="btn btn-secondary" @click="showAdminPanel = true">
          ⚙️ Gestionar POIs
        </button>
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
        <div v-if="resultadoDistanciaVuelo" class="result-box fade-in">
          <span class="label">Total Recorrido:</span>
          <span class="value">{{ resultadoDistanciaVuelo }}</span>
        </div>
      </div>

      <div class="card">
        <div class="card-icon">🎯</div>
        <h3>Proximidad a POI</h3>
        <p>Verifica la distancia mínima a la que pasó el dron.</p>
        <button
          @click="calcularProximidad"
          :disabled="loadingProximidad || !selectedMisionId || !selectedPoiId"
          class="btn btn-warning"
        >
          {{ loadingProximidad ? "Verificando..." : "Verificar Cercanía" }}
        </button>
        <div v-if="resultadoProximidad" class="result-box fade-in">
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
        >
          👁️ Ver Ruta en Mapa
        </button>
      </div>
    </div>

    <div
      v-if="showAdminPanel"
      class="modal-overlay"
      @click.self="showAdminPanel = false"
    >
      <div class="modal-content modal-large">
        <header class="modal-header">
          <h2>
            {{ isEditing ? "Editar Punto de Interés" : "Administrar Puntos" }}
          </h2>
          <button class="close-btn" @click="showAdminPanel = false">✕</button>
        </header>

        <div class="crud-layout">
          <div class="crud-form">
            <div class="form-row">
              <div class="input-group">
                <label>Nombre del Punto *</label>
                <input
                  type="text"
                  v-model="poiForm.nombre"
                  placeholder="Ej: Base Norte"
                />
              </div>
              <div class="input-group">
                <label>Descripción</label>
                <input
                  type="text"
                  v-model="poiForm.descripcion"
                  placeholder="Descripción corta"
                />
              </div>
            </div>

            <div class="form-row">
              <div class="input-group">
                <label>Latitud (Automático)</label>
                <input
                  type="number"
                  v-model="poiForm.latitud"
                  class="input-navy"
                  step="any"
                  placeholder="-33.000000"
                />
              </div>
              <div class="input-group">
                <label>Longitud (Automático)</label>
                <input
                  type="number"
                  v-model="poiForm.longitud"
                  class="input-navy"
                  step="any"
                  placeholder="-70.000000"
                />
              </div>
            </div>

            <div class="mini-map-container">
              <label>📍 Haz clic en el mapa para ubicar el punto:</label>
              <div class="mini-map-wrapper">
                <ClientOnly fallback="Cargando mapa de selección...">
                  <PtoInteresMap
                    :puntos="[]"
                    @map-click="onCoordenadaCapturada"
                  />
                </ClientOnly>
              </div>
            </div>

            <div class="form-actions" style="margin-top: 15px">
              <button
                class="btn btn-success"
                @click="guardarPunto"
                :disabled="loadingSave"
              >
                {{
                  loadingSave
                    ? "Guardando..."
                    : isEditing
                      ? "Actualizar Punto"
                      : "Crear Nuevo Punto"
                }}
              </button>
              <button
                class="btn btn-danger"
                v-if="isEditing"
                @click="resetForm"
              >
                Cancelar Edición
              </button>
            </div>
          </div>

          <div class="crud-list">
            <h3>Puntos Existentes</h3>
            <ul class="poi-list">
              <li v-for="p in puntosInteres" :key="p.poi_id">
                <div class="poi-info">
                  <strong>{{ p.nombre }}</strong>
                  <small
                    >{{ p.latitud.toFixed(4) }},
                    {{ p.longitud.toFixed(4) }}</small
                  >
                </div>
                <div class="poi-actions">
                  <button
                    class="btn-icon edit"
                    @click="editarPunto(p)"
                    title="Editar"
                  >
                    ✏️
                  </button>
                  <button
                    class="btn-icon delete"
                    @click="eliminarPunto(p.poi_id)"
                    title="Eliminar"
                  >
                    🗑️
                  </button>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <div class="map-section">
      <h2>Visualización Geográfica General</h2>
      <div class="map-wrapper">
        <ClientOnly fallback-tag="div" fallback="Cargando mapa...">
          <PtoInteresMap
            ref="mapaRef"
            :misionId="selectedMisionId"
            :poiId="selectedPoiId"
            @map-click="onMapaClick"
          />
        </ClientOnly>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* =========================================
   1. CONFIGURACIÓN GENERAL (Tu base original)
   ========================================= */
.page-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  color: #e2e8f0;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}
.header h1 {
  color: #f8fafc;
  margin-bottom: 10px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}
.subtitle {
  color: #94a3b8;
  font-size: 1.1rem;
}

/* --- ALERTAS --- */
.alert-error {
  background-color: rgba(220, 38, 38, 0.2);
  color: #fca5a5;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  text-align: center;
  border: 1px solid rgba(220, 38, 38, 0.5);
}

/* =========================================
   2. PANEL DE CONTROLES (Actualizado para alinear botón)
   ========================================= */
.control-panel {
  /* Tu estilo Glassmorphism original */
  background-color: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(10px);
  padding: 25px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
  margin-bottom: 30px;

  /* NUEVO: Flexbox para separar inputs del botón derecho */
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

/* Grupo de la izquierda (Selects) */
.panel-inputs {
  display: flex;
  gap: 20px;
  flex: 1;
  flex-wrap: wrap;
}

/* Grupo de la derecha (Botón Admin) */
.panel-actions {
  min-width: 180px; /* Ancho mínimo para el botón */
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
  color: #cbd5e1;
}

/* =========================================
   3. INPUTS (Selects y Textos Unificados)
   ========================================= */
/* Aplicamos tu estilo a .select-input Y a todos los input normales */
.select-input,
input {
  padding: 12px;
  border-radius: 8px;
  font-size: 1rem;
  background-color: #0f172a; /* Fondo oscuro */
  color: #ffffff;
  border: 1px solid #334155;
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
  width: 100%; /* Asegura que llenen el espacio */
  box-sizing: border-box; /* Evita que el padding rompa el ancho */
}

/* Estilo específico solo para selects (la flechita) */
.select-input {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  background-size: 1em;
}

.select-input:focus,
input:focus {
  outline: none;
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.2);
}

.input-navy {
  background-color: #1e3a8a !important; /* Azul Marino */
  color: #ffffff !important; /* Texto Blanco */
  border: 1px solid #3b82f6 !important; /* Borde Azul Brillante */
  font-weight: 600;
  letter-spacing: 0.5px;
}

.input-navy:focus {
  background-color: #172554 !important; /* Un poco más oscuro al hacer clic */
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.4) !important;
}

/* =========================================
   4. CARDS (Tu estilo original)
   ========================================= */
.cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  margin-bottom: 40px;
}

.card {
  background-color: rgba(30, 41, 59, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transition:
    transform 0.2s,
    background-color 0.2s;

  /* Para alinear los botones al fondo siempre */
  display: flex;
  flex-direction: column;
  justify-content: space-between;
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
  flex-grow: 1; /* Empuja el botón hacia abajo */
}

/* =========================================
   5. BOTONES (Extendidos)
   ========================================= */
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

/* TUS BOTONES ORIGINALES */
.btn-primary {
  background-color: #2563eb;
  color: white;
  box-shadow: 0 4px 6px rgba(37, 99, 235, 0.3);
}
.btn-primary:hover:not(:disabled) {
  background-color: #1d4ed8;
}

.btn-warning {
  background-color: #d97706;
  color: white;
  box-shadow: 0 4px 6px rgba(217, 119, 6, 0.3);
}
.btn-warning:hover:not(:disabled) {
  background-color: #b45309;
}

/* NUEVOS BOTONES */
.btn-info {
  /* Azul Cielo para Mapa */
  background-color: #0ea5e9;
  color: white;
  box-shadow: 0 4px 6px rgba(14, 165, 233, 0.3);
}
.btn-info:hover {
  background-color: #0284c7;
}

.btn-secondary {
  /* Gris para Gestionar */
  background-color: #475569;
  color: white;
}
.btn-secondary:hover {
  background-color: #334155;
}

.btn-success {
  /* Verde para Guardar */
  background-color: #10b981;
  color: white;
}
.btn-success:hover {
  background-color: #059669;
}

.btn-danger {
  /* Rojo para Cancelar */
  background-color: #ef4444;
  color: white;
}
.btn-danger:hover {
  background-color: #dc2626;
}

/* =========================================
   6. RESULTADOS (Tu estilo original)
   ========================================= */
.result-box {
  margin-top: 20px;
  background-color: rgba(15, 23, 42, 0.6);
  padding: 15px;
  border-radius: 8px;
  border-left: 5px solid #3b82f6;
  display: flex;
  flex-direction: column;
  align-items: center;
}
/* Pequeña animación */
.fade-in {
  animation: fadeIn 0.5s ease-out;
}

.result-box .label {
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #60a5fa;
}

.result-box .value {
  font-size: 1.5rem;
  font-weight: bold;
  color: #ffffff;
  margin-top: 5px;
  text-shadow: 0 0 10px rgba(59, 130, 246, 0.5);
}

/* =========================================
   7. MODAL / POPUP (NUEVO)
   ========================================= */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.85); /* Fondo muy oscuro */
  backdrop-filter: blur(5px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background-color: #1e293b; /* Mismo gris azulado que tus paneles */
  border: 1px solid #475569;
  border-radius: 12px;
  padding: 30px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto; /* Scroll si es muy alto */
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.7);
  animation: scaleUp 0.3s ease-out;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  border-bottom: 1px solid #334155;
  padding-bottom: 15px;
}
.modal-header h2 {
  margin: 0;
  color: #38bdf8;
}
.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 1.5rem;
  cursor: pointer;
}
.close-btn:hover {
  color: #fff;
}

/* Layout Grid dentro del Modal (Form a la izq, Lista a la der) */
.crud-layout {
  display: grid;
  grid-template-columns: 2fr 1fr; /* Formulario ancho, Lista estrecha */
  gap: 30px;
}
@media (max-width: 768px) {
  .crud-layout {
    grid-template-columns: 1fr;
  } /* Columna simple en movil */
}

/* Formulario */
.crud-form .form-row {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}
.crud-form .three-cols .input-group {
  flex: 1;
}
.input-group {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.input-group label {
  font-size: 0.9rem;
  color: #94a3b8;
  margin-bottom: 5px;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 25px;
}

/* Lista Lateral */
.crud-list {
  background-color: rgba(15, 23, 42, 0.5);
  padding: 15px;
  border-radius: 8px;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #334155;
}
.crud-list h3 {
  margin-top: 0;
  font-size: 1.1rem;
  color: #cbd5e1;
  margin-bottom: 15px;
}

.poi-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.poi-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #334155;
}
.poi-list li:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.poi-info {
  display: flex;
  flex-direction: column;
}
.poi-info strong {
  color: #e2e8f0;
  font-size: 0.95rem;
}
.poi-info small {
  color: #64748b;
  font-size: 0.8rem;
}

.poi-actions {
  display: flex;
  gap: 5px;
}
.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.1rem;
  padding: 5px;
  transition: transform 0.2s;
}
.btn-icon:hover {
  transform: scale(1.2);
}

/* =========================================
   8. MAPA (Ajustado para que NO se vea en blanco)
   ========================================= */
.map-section h2 {
  color: #f1f5f9;
  margin-bottom: 15px;
}

.map-wrapper {
  display: block; /* Asegura que sea bloque */
  width: 100%;
  height: 600px !important; /* El !important fuerza la altura si o si */
  background-color: #333; /* Un fondo gris para ver si carga el div aunque no cargue el mapa */
}

/* Contenedor del mapa pequeño dentro del modal */
.mini-map-container {
  margin-top: 10px;
  border: 1px solid #475569; /* Ajusté el color al tema oscuro */
  border-radius: 8px;
  overflow: hidden;
}

.mini-map-wrapper {
  display: block;
  width: 100%;
  height: 300px !important;
  background-color: #333;
}
/* Ajuste para que el modal sea un poco más ancho */
.modal-large {
  max-width: 900px;
  width: 95%;
}

/* Animaciones */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@keyframes scaleUp {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
