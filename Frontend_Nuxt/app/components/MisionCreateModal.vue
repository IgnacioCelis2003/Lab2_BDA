<template>
  <div
    v-if="props.show"
    class="modal"
    style="
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 40;
    "
  >
    <div
      class="card"
      style="
        max-width: 640px;
        width: min(95vw, 640px);
        max-height: 80vh;
        overflow: auto;
        padding: 1rem;
        box-sizing: border-box;
        background: rgba(31, 31, 52, 0.98);
        border-radius: 8px;
        border: 1px solid rgba(0, 0, 0, 0.12);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
      "
    >
      <h3>
        {{
          isEdit ? `Editar Misión #${props.mision?.idMision}` : "Nueva Misión"
        }}
      </h3>

      <form @submit.prevent="submit">
        <!-- Tipo de misión -->
        <label>
          Tipo de Misión
          <select
            v-model.number="form.idTipoMision"
            required
            :disabled="saving"
          >
            <option v-if="!tiposMision && !tiposError" disabled>
              Cargando tipos...
            </option>
            <option v-if="tiposError" disabled>Error cargando tipos</option>
            <option
              v-for="t in tiposMision"
              :key="t.idTipoMision"
              :value="t.idTipoMision"
            >
              {{ t.nombreTipo }}
            </option>
          </select>
        </label>

        <!-- Dron asignado -->
        <label>
          Dron Asignado
          <select
            v-model.number="form.idDronAsignado"
            required
            :disabled="saving"
          >
            <option v-if="!drones && !dronesError" disabled>
              Cargando drones...
            </option>
            <option v-if="dronesError" disabled>Error cargando drones</option>
            <option v-for="d in drones" :key="d.idDron" :value="d.idDron">
              Dron {{ d.idDron }} (Modelo
              {{ modelsMap[d.idModelo]?.nombreModelo }})
            </option>
          </select>
        </label>

        <!-- Fechas -->
        <label>
          Fecha Inicio Planificada
          <input
            v-model="form.fechaInicioPlanificada"
            type="datetime-local"
            required
            :disabled="saving"
          />
        </label>

        <label>
          Fecha Fin Planificada
          <input
            v-model="form.fechaFinPlanificada"
            type="datetime-local"
            required
            :disabled="saving"
          />
        </label>

        <!-- Estado -->
        <label>
          Estado
          <select v-model="form.estado" required :disabled="saving">
            <option>Pendiente</option>
            <option>Completada</option>
          </select>
        </label>

        <!-- Ruta -->
        <label>
          Ruta (selecciona 2 puntos en el mapa)
          <MapPicker mode="route" v-model:rutaWKT="form.rutaWKT" />

          <div style="margin-top: 0.5rem">
            <small>Ruta WKT generada:</small>
            <div
              style="
                font-size: 0.85rem;
                word-break: break-word;
                background: #0f1724;
                padding: 0.5rem;
                border-radius: 4px;
                margin-top: 0.25rem;
              "
            >
              {{ form.rutaWKT || "Sin ruta (haz doble clic para limpiar)" }}
            </div>
          </div>
        </label>

        <!-- Acciones -->
        <div
          style="
            display: flex;
            gap: 0.5rem;
            justify-content: flex-end;
            margin-top: 1rem;
          "
        >
          <button
            type="button"
            class="secondary"
            @click="cancel"
            :disabled="saving"
          >
            Cancelar
          </button>
          <button type="submit" :disabled="saving" class="contrast">
            {{ saving ? "Guardando..." : isEdit ? "Guardar" : "Crear" }}
          </button>
        </div>

        <p v-if="formError" class="error" style="color: var(--error)">
          {{ formError }}
        </p>
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

      // ISO -> datetime-local: yyyy-MM-ddTHH:mm
      const toLocalInput = (iso: string) => (iso ? iso.slice(0, 16) : "");
      form.fechaInicioPlanificada = toLocalInput(
        props.mision.fechaInicioPlanificada
      );
      form.fechaFinPlanificada = toLocalInput(props.mision.fechaFinPlanificada);

      form.estado = props.mision.estado || "Pendiente";
      form.rutaWKT = props.mision.rutaWKT || "";
    } else {
      // crear
      form.idTipoMision = 0;
      form.idDronAsignado = null;
      form.fechaInicioPlanificada = "";
      form.fechaFinPlanificada = "";
      form.estado = "Pendiente";
      form.rutaWKT = "";
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

  if (!form.rutaWKT || !form.rutaWKT.toUpperCase().startsWith("LINESTRING")) {
    formError.value =
      "Debes ingresar una ruta válida en formato WKT (LINESTRING)";
    return;
  }

  const payload = {
    idTipoMision: form.idTipoMision,
    idDronAsignado: form.idDronAsignado,
    fechaInicioPlanificada: new Date(form.fechaInicioPlanificada).toISOString(),
    fechaFinPlanificada: new Date(form.fechaFinPlanificada).toISOString(),
    estado: form.estado,
    rutaWKT: form.rutaWKT,
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
  .card {
    padding: 0.75rem;
  }
}
</style>
