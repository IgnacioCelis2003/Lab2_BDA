<script lang="ts" setup>
import { ref } from "vue";

// Interface para Misión
interface Mision {
  idMision: number;
  idDronAsignado: number | null;
  idTipoMision: number;
  idOperadorCreador: number;
  fechaCreacion: string;
  fechaInicioPlanificada: string;
  fechaFinPlanificada: string;
  fechaInicioReal: string | null;
  fechaFinReal: string | null;
  estado: string;
  rutaWKT: string;
}

// Obtener todas las misiones registradas (backend: GET /api/misiones)
const {
  data: misiones,
  error,
  status,
  refresh,
} = await useFetch<Mision[]>(`/api/misiones/all`);

// Modal crear
const showModal = ref(false);

// Modal editar
const showEditModal = ref(false);
const selectedMision = ref<Mision | null>(null);

// Estado para botón eliminar
const deletingId = ref<number | null>(null);

// Estado para botón iniciar misión
const startingId = ref<number | null>(null);

// Manejo del evento de crear misión
async function onCreated() {
  showModal.value = false;
  await refresh();
}

function openEdit(m: Mision) {
  selectedMision.value = { ...m };
  showEditModal.value = true;
}

async function onSaved() {
  showEditModal.value = false;
  selectedMision.value = null;
  await refresh();
}

async function deleteMision(m: Mision) {
  const id = m?.idMision;
  if (!id) return;

  const ok = confirm(`¿Seguro que quieres eliminar la misión #${id}?`);
  if (!ok) return;

  deletingId.value = id;

  try {
    await $fetch(`/api/misiones/eliminar/${id}`, {
      method: "DELETE",
    });

    // si justo estabas editando esa misión, cierra modal
    if (selectedMision.value?.idMision === id) {
      showEditModal.value = false;
      selectedMision.value = null;
    }

    await refresh();
  } catch (e: any) {
    console.error("Failed deleting mision", e);
    alert(e?.data?.message || e?.message || "Error eliminando misión");
  } finally {
    deletingId.value = null;
  }
}

async function iniciarMision(m: Mision) {
  const id = m?.idMision;
  if (!id) return;

  const ok = confirm(`¿Seguro que quieres iniciar la misión #${id}?`);
  if (!ok) return;

  startingId.value = id;

  try {
    await $fetch(`/api/misiones/iniciar/${id}`, {
      method: "POST",
    });

    await refresh();
  } catch (e: any) {
    console.error("Failed starting mision", e);
    alert(e?.data?.message || e?.message || "Error iniciando misión");
  } finally {
    startingId.value = null;
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
        <h2 style="margin: 0;">Misiones</h2>
        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
          <button class="contrast btn-compact" type="button" @click="showModal = true">
            Crear misión
          </button>
          <NuxtLink to="/tiposmisiones" type="button" class="secondary btn-compact">
            Revisar misiones disponibles
          </NuxtLink>
          <NuxtLink to="/desempenotipomisiones" type="button" class="contrast btn-compact">
            Revisar Desempeño por Tipo de Misión
          </NuxtLink>
          <NuxtLink to="/desempenomensual" type="button" class="contrast btn-compact">
            Revisar Desepeño Mensual de Misiones
          </NuxtLink>
          <NuxtLink to="/consumobateriamisiones" type="button" class="contrast btn-compact">
            Revisar Consumo de Batería por Misión
          </NuxtLink>
        </div>
      </div>
    </div>

    <article v-if="status === 'pending'" aria-busy="true" />

    <article class="error" v-else-if="error">
      {{ error.statusMessage || error.message }}
    </article>

    <section v-else class="misiones-grid">
      <article v-for="m in misiones" :key="m.idMision" class="card">
        <h3>Misión #{{ m.idMision }}</h3>

        <p><strong>Tipo de Misión:</strong> ID {{ m.idTipoMision }}</p>

        <p>
          <strong>Dron Asignado:</strong>
          {{ m.idDronAsignado ? `ID ${m.idDronAsignado}` : "No asignado" }}
        </p>

        <p>
          <strong>Estado: </strong>
          <span
              :class="
                m.estado === 'Pendiente'
                  ? 'status-pending'
                  : m.estado === 'En Progreso'
                  ? 'status-in-progress'
                  : m.estado === 'Completada'
                  ? 'status-completed'
                  : m.estado === 'Fallida'
                  ? 'status-failed'
                  : ''
              "
            >
            {{ m.estado }}
          </span>
        </p>

        <hr style="margin: 0.5rem 0" />

        <p>
          <strong>Fecha Inicio Planificada:</strong>
          {{ new Date(m.fechaInicioPlanificada).toLocaleString("es-CL") }}
        </p>

        <p>
          <strong>Fecha Fin Planificada:</strong>
          {{ new Date(m.fechaFinPlanificada).toLocaleString("es-CL") }}
        </p>

        <p v-if="m.fechaInicioReal">
          <strong>Fecha Inicio Real:</strong>
          {{ new Date(m.fechaInicioReal).toLocaleString("es-CL") }}
        </p>

        <p v-if="m.fechaFinReal">
          <strong>Fecha Fin Real:</strong>
          {{ new Date(m.fechaFinReal).toLocaleString("es-CL") }}
        </p>

        <hr style="margin: 0.5rem 0" />

        <p>
          <strong>Ruta WKT:</strong>
          <code style="font-size: 0.85rem; word-break: break-word">{{
            m.rutaWKT
          }}</code>
        </p>

        <p>
          <strong>Creada:</strong>
          {{ new Date(m.fechaCreacion).toLocaleString("es-CL") }}
        </p>

        <div style="display: flex; gap: 0.5rem; margin-top: 0.75rem">
          <button
            v-if="m.estado === 'Pendiente'"
            class="primary"
            :disabled="startingId === m.idMision"
            :aria-busy="startingId === m.idMision"
            @click="iniciarMision(m)"
          >
            {{ startingId === m.idMision ? "Iniciando..." : "Iniciar Misión" }}
          </button>

          <button class="secondary" @click="openEdit(m)">Editar</button>

          <button
            class="contrast"
            :disabled="deletingId === m.idMision"
            :aria-busy="deletingId === m.idMision"
            @click="deleteMision(m)"
          >
            {{ deletingId === m.idMision ? "Eliminando..." : "Eliminar" }}
          </button>
        </div>
      </article>
    </section>

    <!-- Modal crear -->
    <MisionCreateModal v-model:show="showModal" @created="onCreated" />

    <!-- Modal editar (debes crear este componente) -->
    <MisionCreateModal
      v-model:show="showEditModal"
      :mision="selectedMision"
      @saved="onSaved"
    />
  </main>
</template>

<style scoped>
/* Esto crea una grilla responsive.
  - minmax(300px, 1fr): Cada tarjeta tendrá al menos 300px de ancho.
  - auto-fill: Si caben más tarjetas en la pantalla, las pone al lado.
    Si no caben, las baja a la siguiente línea.
*/
.misiones-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem; /* Espacio entre tarjetas */
  margin-top: 1rem;
}

/* Opcional: Para asegurar que las tarjetas tengan altura uniforme */
.card {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.btn-compact {
  padding: 0.45rem 0.6rem;
  font-size: 0.85rem;
  white-space: nowrap;
  height: auto;
  display: inline-flex;
  align-items: center;
}

.status-pending {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}

.status-in-progress {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}

.status-completed {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}

.status-failed {
  background: rgba(220, 38, 38, 0.15);
  color: #dc2626;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}
</style>
