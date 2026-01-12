<script setup lang="ts">
import { ref, watch, computed } from "vue";

type Modelo = {
  idModelo: number;
  nombreModelo: string;
  fabricante: string;
  capacidadCargaKg: number;
  autonomiaMinutos: number;
  velocidadPromedioKmh: number;
};

const props = defineProps({
  show: { type: Boolean, default: false },
  modelo: { type: Object as () => Modelo | null, default: null },
});

const emit = defineEmits(["update:show", "created", "saved"]);

const nombreModelo = ref("");
const fabricante = ref("");
const capacidadCargaKg = ref<number | null>(null);
const autonomiaMinutos = ref<number | null>(null);
const velocidadPromedioKmh = ref<number | null>(null);
const submitting = ref(false);
const error = ref<string | null>(null);

const isEdit = computed(() => !!props.modelo?.idModelo);

function close() {
  emit("update:show", false);
}

watch(
  [() => props.show, () => props.modelo],
  () => {
    if (!props.show) return;

    error.value = null;

    if (isEdit.value && props.modelo) {
      nombreModelo.value = props.modelo.nombreModelo;
      fabricante.value = props.modelo.fabricante;
      capacidadCargaKg.value = props.modelo.capacidadCargaKg;
      autonomiaMinutos.value = props.modelo.autonomiaMinutos;
      velocidadPromedioKmh.value = props.modelo.velocidadPromedioKmh;
    } else {
      nombreModelo.value = "";
      fabricante.value = "";
      capacidadCargaKg.value = null;
      autonomiaMinutos.value = null;
      velocidadPromedioKmh.value = null;
    }
  },
  { immediate: true }
);

async function submit() {
  error.value = null;

  if (
    !nombreModelo.value ||
    !fabricante.value ||
    capacidadCargaKg.value == null ||
    autonomiaMinutos.value == null ||
    velocidadPromedioKmh.value == null
  ) {
    error.value = "Todos los campos son obligatorios";
    return;
  }

  submitting.value = true;

  try {
    const body = {
      nombreModelo: nombreModelo.value,
      fabricante: fabricante.value,
      capacidadCargaKg: capacidadCargaKg.value,
      autonomiaMinutos: autonomiaMinutos.value,
      velocidadPromedioKmh: velocidadPromedioKmh.value,
    };

    if (isEdit.value && props.modelo) {
      await $fetch(`/api/modelos/actualizar/${props.modelo.idModelo}`, {
        method: "PUT",
        body,
      });
      emit("saved");
      close();
      return;
    }

    await $fetch("/api/modelos/crear", {
      method: "POST",
      body,
    });

    emit("created");
    close();
  } catch (err: any) {
    error.value = err?.data?.message || err?.message || String(err);
  } finally {
    submitting.value = false;
  }
}
</script>

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
        background: rgba(31, 31, 52, 0.98);
        border-radius: 8px;
      "
    >
      <h3>
        {{
          isEdit ? `Editar Modelo #${props.modelo?.idModelo}` : "Agregar Modelo"
        }}
      </h3>

      <form @submit.prevent="submit" class="stack">
        <label>
          Nombre del modelo
          <input v-model="nombreModelo" type="text" />
        </label>

        <label>
          Fabricante
          <input v-model="fabricante" type="text" />
        </label>

        <label>
          Capacidad (kg)
          <input v-model.number="capacidadCargaKg" type="number" step="0.1" />
        </label>

        <label>
          Autonomía (min)
          <input v-model.number="autonomiaMinutos" type="number" />
        </label>

        <label>
          Velocidad Promedio (Km/h)
          <input
            v-model.number="velocidadPromedioKmh"
            type="number"
            step="0.1"
          />
        </label>

        <div style="display: flex; gap: 0.5rem; justify-content: flex-end">
          <button type="button" class="secondary" @click="close">
            Cancelar
          </button>
          <button type="submit" class="primary" :disabled="submitting">
            {{ submitting ? "Guardando..." : "Guardar" }}
          </button>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.error {
  color: #b00020;
}
</style>
