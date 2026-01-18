<template>
  <dialog :open="show" @click.self="$emit('update:show', false)">
    <article>
      <header>
        <button
          aria-label="Close"
          rel="prev"
          @click="$emit('update:show', false)"
        />
        <h3>Crear Nueva Zona Prohibida</h3>
      </header>

      <form @submit.prevent="crearZona">
        <div class="form-group">
          <label for="nombre">Nombre:</label>
          <input
            id="nombre"
            v-model="form.nombre"
            type="text"
            placeholder="Ej: Zona Restringida Centro"
            required
          />
        </div>

        <div class="form-group">
          <label>Área (Polygon WKT):</label>
          <textarea
            v-model="form.wkt"
            placeholder="Ej: POLYGON((lon1 lat1, lon2 lat2, lon3 lat3, lon1 lat1))"
            required
            rows="4"
          ></textarea>
          <small>O selecciona en el mapa debajo</small>
        </div>

        <div v-if="error" role="alert">
          {{ error }}
        </div>
        <div v-if="success" role="status">
          Zona prohibida creada exitosamente
        </div>

        <div class="map-section">
          <h4>Selecciona un polígono en el mapa</h4>
          <ClientOnly>
            <PolygonPicker @polygon-selected="onPolygonSelected" />
          </ClientOnly>
        </div>
      </form>

      <footer>
        <button
          type="button"
          class="secondary"
          @click="$emit('update:show', false)"
        >
          Cancelar
        </button>
        <button
          type="submit"
          :disabled="isLoading"
          :aria-busy="isLoading"
          @click="crearZona"
        >
          {{ isLoading ? "Creando..." : "Crear Zona" }}
        </button>
      </footer>
    </article>
  </dialog>
</template>

<script setup lang="ts">
import { ref } from "vue";
import PolygonPicker from "~/components/Lab2/PolygonPicker.vue";

interface ZonaProhibida {
  nombre: string;
  wkt: string;
}

interface Props {
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
});

const emit = defineEmits<{
  (e: "update:show", value: boolean): void;
  (e: "created"): void;
}>();

const form = ref<ZonaProhibida>({
  nombre: "",
  wkt: "",
});

const isLoading = ref(false);
const error = ref("");
const success = ref(false);

const crearZona = async () => {
  try {
    isLoading.value = true;
    error.value = "";
    success.value = false;

    // Validar nombre
    if (!form.value.nombre.trim()) {
      throw new Error("El nombre de la zona es requerido");
    }

    // Validar que el polígono sea válido
    if (!form.value.wkt || !form.value.wkt.trim().startsWith("POLYGON")) {
      throw new Error("El área debe ser un polígono válido en formato WKT");
    }

    console.log("[ZonasProhibidasCreate] enviando:", {
      nombre: form.value.nombre,
      wkt: form.value.wkt,
    });

    const response = await $fetch("/api/zonas/crear", {
      method: "POST",
      body: {
        nombre: form.value.nombre,
        wkt: form.value.wkt,
      },
    });

    if (response) {
      success.value = true;
      setTimeout(() => {
        resetForm();
        emit("update:show", false);
        emit("created");
      }, 1500);
    }
  } catch (err: any) {
    console.error("[ZonasProhibidasCreate] Error:", err);
    console.error("[ZonasProhibidasCreate] Error data:", err?.data);
    console.error("[ZonasProhibidasCreate] Error message:", err?.message);

    error.value =
      err?.data?.message ||
      err?.message ||
      "Error al crear la zona prohibida";
  } finally {
    isLoading.value = false;
  }
};

const onPolygonSelected = (polygonWkt: string) => {
  form.value.wkt = polygonWkt;
  console.log("[ZonasProhibidasCreate] Polígono seleccionado:", polygonWkt);
};

const resetForm = () => {
  form.value = {
    nombre: "",
    wkt: "",
  };
  error.value = "";
  success.value = false;
};
</script>

<style scoped>
dialog {
  border: none;
  border-radius: 0.375rem;
  max-width: 90vw;
  height: fit-content;
  max-height: 90vh;
  overflow: visible;
}

dialog::backdrop {
  background: rgba(0, 0, 0, 0.5);
}

article {
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

header h3 {
  margin: 0;
  font-size: 1.25rem;
}

header button {
  width: 30px;
  height: 30px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

form {
  padding: 1.5rem;
  overflow-y: auto;
  max-height: calc(90vh - 120px);
  flex-shrink: 1;
  min-height: 0;
}

.form-group {
  margin-bottom: 1rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

input,
textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 0.375rem;
  font-family: inherit;
  box-sizing: border-box;
}

textarea {
  resize: vertical;
  font-family: "Courier New", monospace;
  font-size: 0.875rem;
}

small {
  display: block;
  margin-top: 0.25rem;
  opacity: 0.7;
  font-size: 0.875rem;
}

[role="alert"] {
  background: rgba(220, 53, 69, 0.15);
  color: #dc3545;
  padding: 0.75rem;
  border-radius: 0.375rem;
  margin-bottom: 1rem;
  border-left: 4px solid #dc3545;
}

[role="status"] {
  background: rgba(40, 167, 69, 0.15);
  color: #28a745;
  padding: 0.75rem;
  border-radius: 0.375rem;
  margin-bottom: 1rem;
  border-left: 4px solid #28a745;
}

.map-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(0, 0, 0, 0.12);
}

.map-section h4 {
  margin: 0 0 1rem 0;
  font-size: 1rem;
}

footer {
  border-top: 1px solid rgba(0, 0, 0, 0.12);
  padding: 1rem;
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  flex-shrink: 0;
}

footer button {
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
}
</style>
