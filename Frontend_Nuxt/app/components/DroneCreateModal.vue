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
      <h3 v-if="isEdit">Editar Dron #{{ props.dron?.idDron }}</h3>
      <h3 v-else>Nuevo Dron</h3>

      <form @submit.prevent="save">
        <label>
          Modelo
          <select v-model.number="form.idModelo" required>
            <option v-if="!modelos && !modelosError" disabled>
              Cargando modelos...
            </option>
            <option v-if="modelosError" disabled>Error cargando modelos</option>
            <option v-for="m in modelos" :key="m.idModelo" :value="m.idModelo">
              {{ m.nombreModelo }} — {{ m.fabricante }}
            </option>
          </select>
        </label>

        <label>
          Estado
          <select v-model="form.estado">
            <option>Disponible</option>
            <option>En Mantenimiento</option>
          </select>
        </label>

        <div
          style="
            display: flex;
            gap: 0.5rem;
            justify-content: flex-end;
            margin-top: 1rem;
          "
        >
          <button type="button" class="secondary" @click="cancel">
            Cancelar
          </button>
          <button type="submit" :disabled="saving" class="contrast">
            {{ saving ? "Guardando..." : isEdit ? "Guardar cambios" : "Crear" }}
          </button>
        </div>

        <p class="error" v-if="formError" style="color: var(--error)">
          {{ formError }}
        </p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";

type Dron = {
  idDron: number;
  idModelo: number;
  estado: string;
};

const props = defineProps<{
  show: boolean;
  dron?: Dron | null;
}>();

const emit = defineEmits(["update:show", "created", "saved"]);

const saving = ref(false);
const formError = ref<string | null>(null);

interface Model {
  idModelo: number;
  nombreModelo: string;
  fabricante: string;
  capacidadCargaKg: number;
  autonomiaMinutos: number;
}

const { data: modelos, error: modelosError } = await useFetch<Model[]>(
  "/api/modelos/all"
);

const isEdit = computed(() => !!props.dron?.idDron);

const form = reactive({
  idModelo: 0,
  estado: "Disponible",
});

// Precargar valores cuando se abre
watch(
  () => [props.show, props.dron, modelos.value],
  () => {
    if (!props.show) return;

    formError.value = null;

    if (isEdit.value && props.dron) {
      form.idModelo = props.dron.idModelo;
      form.estado = props.dron.estado;
      return;
    }

    // modo crear
    form.estado = "Disponible";
    if (modelos.value?.length) form.idModelo = modelos.value[0].idModelo;
  },
  { immediate: true }
);

function cancel() {
  emit("update:show", false);
}

async function save() {
  formError.value = null;

  if (!["Disponible", "En Mantenimiento"].includes(form.estado)) {
    formError.value =
      'El campo estado debe ser "Disponible" o "En Mantenimiento"';
    return;
  }
  if (!form.idModelo) {
    formError.value = "Debes seleccionar un modelo";
    return;
  }

  saving.value = true;

  try {
    const payload = {
      idModelo: form.idModelo,
      estado: form.estado,
    };

    if (isEdit.value && props.dron) {
      //ENDPOINT REAL
      await $fetch(
        `/api/drones/actualizar/${props.dron.idDron}`,
        {
          method: "PUT",
          body: {
            idModelo: form.idModelo,
            estado: form.estado,
          },
        }
      );
      emit("saved");
    } else {
      await $fetch(`/api/drones/crear`, {
        method: "POST",
        body: {
          idModelo: form.idModelo,
          estado: form.estado,
        },
      });
      emit("created");
    }

    emit("update:show", false);
  } catch (e: any) {
    formError.value = e?.data?.message || e?.message || "Error al guardar";
  } finally {
    saving.value = false;
  }
}
</script>