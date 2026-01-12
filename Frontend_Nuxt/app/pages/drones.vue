<script lang="ts" setup>
import { ref } from "vue";

const {
  data: drones,
  error,
  status,
  refresh,
} = await useFetch("/api/drones/all");

// Mapear idModelo a información del modelo
const modelsMap = ref<Record<number, any>>({});
const modelsLoading = ref(false);

// Modal crear
const showModal = ref(false);

// Modal editar
const showEditModal = ref(false);
const selectedDron = ref<any | null>(null);

// Estado para botón eliminar
const deletingId = ref<number | null>(null);

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

await loadModels();

async function onCreated() {
  showModal.value = false;
  await refresh();
  await loadModels();
}

function openEdit(d: any) {
  selectedDron.value = { ...d };
  showEditModal.value = true;
}

async function onSaved() {
  showEditModal.value = false;
  selectedDron.value = null;
  await refresh();
  await loadModels();
}

// Eliminar dron
async function deleteDron(d: any) {
  const idDron = d?.idDron;
  if (!idDron) return;

  const ok = confirm(`¿Seguro que quieres eliminar el dron #${idDron}?`);
  if (!ok) return;

  deletingId.value = idDron;

  try {
    await $fetch(`/api/drones/eliminar/${idDron}`, {
      method: "DELETE",
    });

    // si justo estabas editando ese dron, cierra modal
    if (selectedDron.value?.idDron === idDron) {
      showEditModal.value = false;
      selectedDron.value = null;
    }

    await refresh();
    await loadModels();
  } catch (e: any) {
    alert(e?.data?.message || e?.message || "Error al eliminar dron");
  } finally {
    deletingId.value = null;
  }
}
</script>

<template>
  <main class="container">
    <div class="stack">
      <div
        style="
          display: flex;
          flex-direction: column;
          gap: 1rem;
          margin-bottom: 2rem;
        "
      >
        <h2 style="margin: 0;">Drones</h2>
        <div style="display: flex; gap: 0.5rem">
          <button class="contrast btn-compact" type="button" @click="showModal = true">
            Agregar Dron
          </button>
          <NuxtLink to="/modelosdron" type="button" class="secondary btn-compact">
            Revisar Modelos Disponibles
          </NuxtLink>
          <NuxtLink to="/dronesconfallos" type="button" class="contrast btn-compact">
            Revisar Drones con más fallos
          </NuxtLink>
          <NuxtLink to="/dronesinactivos" type="button" class="contrast btn-compact">
            Revisar Drones Inactivos
          </NuxtLink>
          <NuxtLink to="/reporteglobal" type="button" class="contrast btn-compact">
            Revisar Reporte Global
          </NuxtLink>
          <NuxtLink to="/cercanos" type="button" class="contrast btn-compact">
            Análisis por puntos de interés
          </NuxtLink>
        </div>
      </div>
    </div>

    <article v-if="status === 'pending' || modelsLoading" aria-busy="true" />

    <article class="error" v-else-if="error">
      {{ error.statusMessage || error.message }}
    </article>

    <section v-else class="drones-grid">
      <article v-for="d in drones" :key="d.idDron" class="card drone-card">
        <div class="card-header">
          <h3 style="margin: 0">Dron #{{ d.idDron }}</h3>
        </div>

        <div class="card-content">
          <p class="info-row"><strong>Modelo ID:</strong> {{ d.idModelo }}</p>
          <p class="info-row" v-if="modelsMap[d.idModelo]">
            <strong>Modelo:</strong> {{ modelsMap[d.idModelo].nombreModelo }}
          </p>
          <p class="info-row" v-else><em>Cargando información...</em></p>

          <p class="info-row">
            <strong>Estado:</strong>
            <span
              :class="
                d.estado === 'Disponible'
                  ? 'status-available'
                  : 'status-maintenance'
              "
            >
              {{ d.estado }}
            </span>
          </p>

          <div v-if="modelsMap[d.idModelo]" class="model-details">
            <p class="info-row">
              <strong>Fabricante:</strong>
              {{ modelsMap[d.idModelo].fabricante }}
            </p>
            <p class="info-row">
              <strong>Capacidad:</strong>
              {{ modelsMap[d.idModelo].capacidadCargaKg }} kg
            </p>
            <p class="info-row">
              <strong>Autonomía:</strong>
              {{ modelsMap[d.idModelo].autonomiaMinutos }} min
            </p>
            <p class="info-row">
              <strong>Velocidad (promedio):</strong>
              {{ modelsMap[d.idModelo].velocidadPromedioKmh }} km/h
            </p>
          </div>
        </div>

        <div class="card-actions">
          <button class="secondary" @click="openEdit(d)">Editar</button>
          <button
            class="contrast"
            :disabled="deletingId === d.idDron"
            :aria-busy="deletingId === d.idDron"
            @click="deleteDron(d)"
          >
            {{ deletingId === d.idDron ? "Eliminando..." : "Eliminar" }}
          </button>
        </div>
      </article>
    </section>

    <!-- Modal crear -->
    <DroneCreateModal v-model:show="showModal" @created="onCreated" />

    <!-- Modal editar -->
    <DroneCreateModal
      v-model:show="showEditModal"
      :dron="selectedDron"
      @saved="onSaved"
    />
  </main>
</template>

<style scoped>
.drones-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}

@media (max-width: 768px) {
  .drones-grid {
    grid-template-columns: 1fr;
  }
}

.drone-card {
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

.status-available {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}

.status-maintenance {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}

.model-details {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
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
