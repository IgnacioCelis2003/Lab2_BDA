Agregar campos numéricos en el formulario para la altitud de inicio y fin.

Interceptar el WKT que viene del mapa (que es 2D) y combinarlo con esas altitudes antes de enviarlo al backend.

Parsear el WKT cuando se edita una misión existente para extraer las altitudes guardadas y mostrarlas en los inputs.
HTML
<template>
  <div
    v-if="props.show"
    class="modal"
    style="position: fixed; inset: 0; background: rgba(0, 0, 0, 0.4); display: flex; align-items: center; justify-content: center; z-index: 40;"
  >
    <div
      class="card"
      style="max-width: 640px; width: min(95vw, 640px); max-height: 80vh; overflow: auto; padding: 1rem; box-sizing: border-box; background: rgba(31, 31, 52, 0.98); border-radius: 8px; border: 1px solid rgba(0, 0, 0, 0.12); box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);"
    >
      <h3>{{ isEdit ? `Editar Misión #${props.mision?.idMision}` : "Nueva Misión" }}</h3>

      <form @submit.prevent="submit">
        <label>
          Tipo de Misión
          <select v-model.number="form.idTipoMision" required :disabled="saving">
            <option v-if="!tiposMision && !tiposError" disabled>Cargando tipos...</option>
            <option v-if="tiposError" disabled>Error cargando tipos</option>
            <option v-for="t in tiposMision" :key="t.idTipoMision" :value="t.idTipoMision">
              {{ t.nombreTipo }}
            </option>
          </select>
        </label>

        <label>
          Dron Asignado
          <select v-model.number="form.idDronAsignado" required :disabled="saving">
            <option v-if="!drones && !dronesError" disabled>Cargando drones...</option>
            <option v-if="dronesError" disabled>Error cargando drones</option>
            <option v-for="d in drones" :key="d.idDron" :value="d.idDron">
              Dron {{ d.idDron }} (Modelo {{ modelsMap[d.idModelo]?.nombreModelo }})
            </option>
          </select>
        </label>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
          <label>
            Fecha Inicio
            <input v-model="form.fechaInicioPlanificada" type="datetime-local" required :disabled="saving" />
          </label>
          <label>
            Fecha Fin
            <input v-model="form.fechaFinPlanificada" type="datetime-local" required :disabled="saving" />
          </label>
        </div>

        <label>
          Ruta (selecciona 2 puntos en el mapa)
          <MapPicker 
            mode="route" 
            v-model:rutaWKT="form.rutaWKT"
            v-model:altitudInicio="form.altitudInicio"
            v-model:altitudFin="form.altitudFin"
          />
        </label>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 0.5rem;">
          <label>
            Altitud Inicio (metros)
            <input 
              v-model.number="form.altitudInicio" 
              type="number" 
              min="0" 
              step="1" 
              required 
              :disabled="saving" 
              placeholder="Automático o manual"
            />
            <small style="color: #888; font-size: 0.7rem;">Detectada automáticamente, editable</small>
          </label>
          <label>
            Altitud Fin (metros)
            <input 
              v-model.number="form.altitudFin" 
              type="number" 
              min="0" 
              step="1" 
              required 
              :disabled="saving" 
              placeholder="Automático o manual"
            />
            <small style="color: #888; font-size: 0.7rem;">Detectada automáticamente, editable</small>
          </label>
        </div>

        <div style="margin-top: 0.5rem">
          <small>Ruta WKT Final (3D):</small>
          <div style="font-size: 0.75rem; word-break: break-word; background: #0f1724; padding: 0.5rem; border-radius: 4px; margin-top: 0.25rem; color: #a5b4fc;">
            {{ wktFinalCalculado || "Selecciona ruta y altitudes..." }}
          </div>
        </div>

        <p v-if="formError" class="error" style="color: #e84848">{{ formError }}</p>

        <div style="display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem;">
          <button type="button" class="secondary" @click="cancel" :disabled="saving">Cancelar</button>
          <button type="submit" :disabled="saving" class="contrast">
            {{ saving ? "Guardando..." : isEdit ? "Guardar" : "Crear" }}
          </button>
        </div>

        
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, watchEffect, computed } from "vue";
import MapPicker from "~/components/MapPicker.vue";

type Mision = {
  idMision: number;
  idDronAsignado: number | null;
  idTipoMision: number;
  fechaInicioPlanificada: string;
  fechaFinPlanificada: string;
  estado: string;
  rutaWKT: string;
};

const props = defineProps({
  show: { type: Boolean, default: false },
  // si viene, es editar; si no viene, es crear
  mision: { type: Object as () => Mision | null, default: null },
});

const emit = defineEmits(["update:show", "created", "saved"]);

const saving = ref(false);
const formError = ref<string | null>(null);

const isEdit = computed(() => !!props.mision?.idMision);

// Interfaces
interface TipoMision {
  idTipoMision: number;
  nombreTipo: string;
}

interface Dron {
  idDron: number;
  nombreDron: string;
  idModelo: number;
}

// Fetch tipos de misión
const { data: tiposMision, error: tiposError } = await useFetch<TipoMision[]>(
  "/api/misiones/tipos/all"
);

// Fetch drones disponibles
const { data: drones, error: dronesError } = await useFetch<Dron[]>(
  "/api/drones/all"
);

// Mapear idModelo a información del modelo
const modelsMap = ref<Record<number, any>>({});
const modelsLoading = ref(false);

// Estado del formulario
const form = reactive({
  idTipoMision: 0,
  idDronAsignado: null as number | null,
  fechaInicioPlanificada: "",
  fechaFinPlanificada: "",
  estado: "Pendiente",
  rutaWKT: "",
  altitudInicio: 100, 
  altitudFin: 100
});

const wktFinalCalculado = computed(() => {
  // Si no hay ruta del mapa, no mostramos nada
  if (!form.rutaWKT || !form.rutaWKT.includes('(')) return "";

  // 1. Limpiamos el string para obtener solo los números
  // Entrada esperada: LINESTRING(-70.1 -33.1, -70.2 -33.2)
  const cleanCoords = form.rutaWKT
    .replace(/LINESTRING\s*\(?/i, '') // Quita el texto inicial
    .replace(')', '') // Quita paréntesis final
    .replace('Z', '') // Por si acaso ya traía Z
    .trim();

  const pairs = cleanCoords.split(','); // Separa los pares de coordenadas

  if (pairs.length < 2) return form.rutaWKT; // Algo anda mal

  // 2. Extraemos Lon y Lat de cada punto
  const p1 = pairs[0].trim().split(' '); // [Lon1, Lat1, (quizas Alt1)]
  const p2 = pairs[1].trim().split(' '); // [Lon2, Lat2, (quizas Alt2)]

  // 3. Construimos el string 3D inyectando las altitudes del formulario
  // Formato: LINESTRING Z (Lon1 Lat1 Alt1, Lon2 Lat2 Alt2)
  return `LINESTRING Z(${p1[0]} ${p1[1]} ${form.altitudInicio}, ${p2[0]} ${p2[1]} ${form.altitudFin})`;
});

// Seleccionar primer tipo de misión automáticamente (solo en crear)
watchEffect(() => {
  if (
    !isEdit.value &&
    tiposMision.value &&
    tiposMision.value.length &&
    form.idTipoMision === 0
  ) {
    form.idTipoMision = tiposMision.value[0].idTipoMision;
  }
});

// Cargar modelos de drones
async function loadModels() {
  modelsMap.value = {};

  if (drones.value && Array.isArray(drones.value) && drones.value.length > 0) {
    modelsLoading.value = true;

    const uniqueIds = Array.from(
      new Set(drones.value.map((d: any) => d.idModelo))
    );

    await Promise.all(
      uniqueIds.map(async (id: number) => {
        try {
          const m = await $fetch(`/api/modelos/${id}`);
          modelsMap.value[id] = m;
        } catch (e) {
          console.warn("[drones] failed fetching model", id, e);
        }
      })
    );

    modelsLoading.value = false;
  }
}

// Cargar modelos al iniciar
watch(drones, async (newDrones) => {
  if (newDrones && newDrones.length > 0) {
    await loadModels();
  }
}, { immediate: true });

// Cuando se abre el modal, precargar valores si es editar
watch(
  [() => props.show, () => props.mision],
  () => {
    if (!props.show) return;
    formError.value = null;

    if (isEdit.value && props.mision) {
      form.idTipoMision = props.mision.idTipoMision;
      form.idDronAsignado = props.mision.idDronAsignado;
      const toLocalInput = (iso: string) => (iso ? iso.slice(0, 16) : "");
      form.fechaInicioPlanificada = toLocalInput(props.mision.fechaInicioPlanificada);
      form.fechaFinPlanificada = toLocalInput(props.mision.fechaFinPlanificada);
      form.estado = props.mision.estado || "Pendiente";
      
      // Lógica de parseo para recuperar altura
      const wkt = props.mision.rutaWKT || "";
      form.rutaWKT = wkt; // El mapa necesita el WKT base

      // Intentamos extraer Z si existe
      if (wkt.includes('Z') || wkt.split(' ')[0].split('(').length > 2) {
         try {
             // Limpiar texto 'LINESTRING Z (' y ')'
             const coordsRaw = wkt.replace(/^[^\(]+\(/, '').replace(/\)$/, '');
             const points = coordsRaw.split(',');
             
             // Punto 1
             const p1Data = points[0].trim().split(' ');
             if (p1Data.length >= 3) form.altitudInicio = parseFloat(p1Data[2]);

             // Punto 2
             const p2Data = points[1].trim().split(' ');
             if (p2Data.length >= 3) form.altitudFin = parseFloat(p2Data[2]);
         } catch (e) {
             console.error("Error parseando altura WKT", e);
         }
      }

    } else {
      // Crear
      form.idTipoMision = 0;
      form.idDronAsignado = null;
      form.fechaInicioPlanificada = "";
      form.fechaFinPlanificada = "";
      form.estado = "Pendiente";
      form.rutaWKT = "";
      form.altitudInicio = 100; // Reset a default
      form.altitudFin = 100;    // Reset a default
    }
  },
  { immediate: true }
);

function cancel() {
  emit("update:show", false);
}

async function submit() {
  formError.value = null;

  // Validaciones
  if (!form.idTipoMision) {
    formError.value = "Debes seleccionar un tipo de misión";
    return;
  }

  if (!form.idDronAsignado) {
    formError.value = "Debes seleccionar un dron";
    return;
  }

  if (!form.fechaInicioPlanificada) {
    formError.value = "Debes ingresar una fecha de inicio";
    return;
  }

  if (!form.fechaFinPlanificada) {
    formError.value = "Debes ingresar una fecha de fin";
    return;
  }

  if (!form.rutaWKT) {
    formError.value = "Debes seleccionar una ruta en el mapa";
    return;
  }

  const rutaFinal3D = wktFinalCalculado.value;

  const payload = {
    idTipoMision: form.idTipoMision,
    idDronAsignado: form.idDronAsignado,
    fechaInicioPlanificada: new Date(form.fechaInicioPlanificada).toISOString(),
    fechaFinPlanificada: new Date(form.fechaFinPlanificada).toISOString(),
    estado: 'Pendiente', // El estado ya no se puede editar manualmente
    rutaWKT: rutaFinal3D,
  };

  saving.value = true;

  try {
    if (isEdit.value && props.mision) {
      await $fetch(`/api/misiones/actualizar/${props.mision.idMision}`, {
        method: "PUT",
        body: payload,
      });

      emit("saved");
      emit("update:show", false);
      return;
    }

    await $fetch("/api/misiones/crear", {
      method: "POST",
      body: payload,
    });

    emit("created");
    emit("update:show", false);
  } catch (e: any) {
    formError.value =
      e?.data?.message || e?.message || "Error guardando misión";
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
@media (max-width: 420px) {
  .card { padding: 0.75rem; }
}

input[type="number"] {
  font-family: monospace;
}
</style>
