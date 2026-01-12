<script setup lang="ts">
import { computed, ref, watch } from "vue";

type TipoMision = {
  idTipoMision: number;
  nombreTipo: string;
};

const props = defineProps({
  show: { type: Boolean, default: false },
  tipo: { type: Object as () => TipoMision | null, default: null },
});

const emit = defineEmits(["update:show", "created", "saved"]);

const nombreTipo = ref("");
const submitting = ref(false);
const error = ref<string | null>(null);

const isEdit = computed(() => !!props.tipo?.idTipoMision);

function close() {
  emit("update:show", false);
}

watch(
  () => [props.show, props.tipo],
  () => {
    if (!props.show) return;

    error.value = null;

    if (isEdit.value && props.tipo) {
      nombreTipo.value = props.tipo.nombreTipo ?? "";
    } else {
      nombreTipo.value = "";
    }
  },
  { immediate: true }
);

async function submit() {
  error.value = null;

  if (!nombreTipo.value.trim()) {
    error.value = "El nombre del tipo es obligatorio";
    return;
  }

  submitting.value = true;

  try {
    const body = {
      nombreTipo: nombreTipo.value.trim(),
    };

    if (isEdit.value && props.tipo) {
      await $fetch(
        `/api/misiones/tipos/actualizar/${props.tipo.idTipoMision}`,
        {
          method: "PUT",
          body,
        }
      );
      emit("saved");
      close();
      return;
    }

    await $fetch("/api/misiones/tipos/crear", {
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
        box-sizing: border-box;
        background: rgba(31, 31, 52, 0.98);
        border-radius: 8px;
        border: 1px solid rgba(0, 0, 0, 0.12);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
      "
    >
      <header class="modal-header" style="margin-bottom: 0.75rem">
        <h3>
          {{
            isEdit
              ? `Editar Tipo de Misión #${props.tipo?.idTipoMision}`
              : "Agregar Tipo de Misión"
          }}
        </h3>
      </header>

      <form @submit.prevent="submit" class="stack">
        <label>
          Nombre del tipo
          <input v-model="nombreTipo" type="text" />
        </label>

        <section style="display: flex; gap: 0.5rem; justify-content: flex-end">
          <button type="button" class="secondary" @click="close">
            Cancelar
          </button>
          <button type="submit" class="primary" :disabled="submitting">
            {{ submitting ? "Guardando..." : "Guardar" }}
          </button>
        </section>

        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>

    <div class="modal-backdrop" @click="close"></div>
  </div>
</template>

<style scoped>
.error {
  color: #b00020;
}
</style>
