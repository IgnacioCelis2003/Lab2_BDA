<script setup lang="ts">
import { ref } from "vue";

const showModal = ref(false);
const showEditModal = ref(false);
const selectedModel = ref<any | null>(null);
const deletingId = ref<number | null>(null);

// Estado para saber qué modelo se está procesando (loading individual)
const loadingMantenimientoId = ref<number | null>(null);

const {
  data: modelos,
  error,
  status,
  refresh,
} = await useFetch("/api/modelos/all");

function onCreated() {
  refresh();
  showModal.value = false;
}

function openEdit(m: any) {
  selectedModel.value = { ...m };
  showEditModal.value = true;
}

function onSaved() {
  refresh();
  showEditModal.value = false;
  selectedModel.value = null;
}

async function deleteModelo(m: any) {
  const id = m?.idModelo;
  if (!id) return;

  const ok = confirm(`¿Seguro que quieres eliminar el modelo #${id}?`);
  if (!ok) return;

  deletingId.value = id;

  try {
    await $fetch(`/api/modelos/eliminar/${id}`, {
      method: "DELETE",
    });
    await refresh();
  } catch (e) {
    console.error("Failed deleting model", e);
    alert("Error eliminando el modelo.");
  } finally {
    deletingId.value = null;
  }
}

async function verificarMantenimiento(m: any) {
  const id = m?.idModelo;
  if (!id) return;

  const confirmacion = confirm(
    `¿Ejecutar revisión masiva para el modelo "${m.nombreModelo}"?\n\n` +
    `Esto pasará a estado 'En Mantenimiento' a todos los drones de este modelo ` +
    `que tengan más de 100 horas de vuelo.`
  );
  
  if (!confirmacion) return;

  loadingMantenimientoId.value = id;

  try {
    // Llamamos a nuestro proxy
    const res: any = await $fetch('/api/drones/mantenimiento/verificar', {
      method: 'POST',
      body: { idModelo: id }
    });

    // Mostramos el resultado que devuelve el backend
    alert(`Proceso finalizado.\n\n${res.mensaje}\nDrones actualizados: ${res.dronesActualizados}`);
    
  } catch (e: any) {
    console.error("Error mantenimiento", e);
    alert(e?.statusMessage || "Error al ejecutar el procedimiento de mantenimiento.");
  } finally {
    loadingMantenimientoId.value = null;
  }
}
</script>

<template>
  <main class="container">
    <div class="stack">
      <div
        style="
          display: flex;
          justify-content: space-between;
          align-items: center;
        "
      >
        <h2>Modelos Disponibles</h2>
        <div style="display: flex; gap: 0.5rem">
          <button class="contrast btn-compact" type="button" @click="showModal = true">
            Agregar Modelo
          </button>
          <NuxtLink to="/drones" type="button" class="secondary btn-compact">Volver</NuxtLink>
          <NuxtLink to="/registroduracionvuelo" type="button" class="contrast btn-compact">
            Revisar Duración de Vuelo
          </NuxtLink>
        </div>
      </div>
    </div>

    <ModelCreateModal v-model:show="showModal" @created="onCreated" />

    <ModelCreateModal
      v-model:show="showEditModal"
      :modelo="selectedModel"
      @saved="onSaved"
    />

    <article v-if="status === 'pending'" aria-busy="true" />

    <article class="error" v-else-if="error">
      {{ error.statusMessage || error.message }}
    </article>

    <section v-else class="models-grid">
      <article v-for="m in modelos" :key="m.idModelo" class="card model-card">
        <div class="card-header">
          <h3 style="margin: 0">{{ m.nombreModelo }}</h3>
        </div>

        <div class="card-content">
          <p class="info-row">
            <strong>Fabricante:</strong> {{ m.fabricante }}
          </p>
          <p class="info-row">
            <strong>Capacidad:</strong> {{ m.capacidadCargaKg }} kg
          </p>
          <p class="info-row">
            <strong>Autonomía:</strong> {{ m.autonomiaMinutos }} min
          </p>
          <p class="info-row">
            <strong>Velocidad (promedio):</strong>
            {{ m.velocidadPromedioKmh }} km/h
          </p>
        </div>

        <div class="card-actions">
          <button class="secondary" @click="openEdit(m)">Editar</button>

          <button 
            class="contrast outline" 
            @click="verificarMantenimiento(m)"
            :aria-busy="loadingMantenimientoId === m.idModelo"
            :disabled="loadingMantenimientoId === m.idModelo"
            title="Enviar a mantenimiento si el dron tiene más de 100h vuelo"
          >
            {{ loadingMantenimientoId === m.idModelo ? "Verificando..." : "Mant. Masivo" }}
          </button>
          
          <button
            class="contrast"
            :disabled="deletingId === m.idModelo"
            :aria-busy="deletingId === m.idModelo"
            @click="deleteModelo(m)"
          >
            {{ deletingId === m.idModelo ? "Eliminando..." : "Eliminar" }}
          </button>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.models-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}

@media (max-width: 768px) {
  .models-grid {
    grid-template-columns: 1fr;
  }
}

.model-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.card-header {
  padding: 1rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.02);
}

.card-content {
  padding: 1rem;
  flex: 1;
  overflow-y: auto;
}

.info-row {
  margin: 0.5rem 0;
  font-size: 0.95rem;
  line-height: 1.4;
}

.info-row strong {
  display: inline-block;
  min-width: 100px;
  color: rgba(255, 255, 255, 0.8);
}

.card-actions {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-top: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.02);
}

.card-actions button {
  flex: 1;
  padding: 0.5rem 0.75rem;
  font-size: 0.9rem;
}

.btn-compact {
  padding: 0.45rem 0.6rem;
  font-size: 0.85rem;
  white-space: nowrap;
  height: auto;
  display: inline-flex;
  align-items: center;
}
</style>
