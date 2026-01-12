<script setup lang="ts">
import MisionTypeCreate from "~/components/MisionTypeCreate.vue";
import { ref } from "vue";

const showModal = ref(false);

const showEditModal = ref(false);
const selectedTipo = ref<any | null>(null);

const deletingId = ref<number | null>(null);

const {
  data: tipos,
  error,
  status,
  refresh,
} = await useFetch(`/api/misiones/tipos/all`);

function onCreated() {
  refresh();
  showModal.value = false;
}

function openEdit(t: any) {
  selectedTipo.value = { ...t };
  showEditModal.value = true;
}

function onSaved() {
  refresh();
  showEditModal.value = false;
  selectedTipo.value = null;
}

async function deleteTipo(t: any) {
  const id = t?.idTipoMision;
  if (!id) return;

  const ok = confirm(`¿Seguro que quieres eliminar el tipo de misión #${id}?`);
  if (!ok) return;

  deletingId.value = id;

  try {
    await $fetch(`/api/tipos-mision/eliminar/${id}`, {
      method: "DELETE",
    });

    if (selectedTipo.value?.idTipoMision === id) {
      showEditModal.value = false;
      selectedTipo.value = null;
    }

    await refresh();
  } catch (e: any) {
    alert(e?.data?.message || e?.message || "Error al eliminar tipo de misión");
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
          justify-content: space-between;
          align-items: center;
        "
      >
        <h2>Tipos de Misión</h2>
        <div style="display: flex; gap: 0.5rem">
          <NuxtLink to="/misiones" type="button" class="secondary btn-compact">Volver</NuxtLink>
          <button class="contrast btn-compact" type="button" @click="showModal = true">
            Agregar Tipo
          </button>
        </div>
      </div>
    </div>

    <MisionTypeCreate v-model:show="showModal" @created="onCreated" />

    <MisionTypeCreate
      v-model:show="showEditModal"
      :tipo="selectedTipo"
      @saved="onSaved"
    />

    <article v-if="status === 'pending'" aria-busy="true" />

    <article class="error" v-else-if="error">
      {{ error.statusMessage || error.message }}
    </article>

    <section v-else class="tipos-grid">
      <article v-for="t in tipos" :key="t.idTipoMision" class="card tipo-card">
        <div class="card-header">
          <h3 style="margin: 0">{{ t.nombreTipo }}</h3>
        </div>

        <div class="card-content">
          <p class="info-row"><strong>ID:</strong> {{ t.idTipoMision }}</p>
        </div>

        <div class="card-actions">
          <button class="secondary" @click="openEdit(t)">Editar</button>
          <button
            class="contrast"
            :disabled="deletingId === t.idTipoMision"
            :aria-busy="deletingId === t.idTipoMision"
            @click="deleteTipo(t)"
          >
            {{ deletingId === t.idTipoMision ? "Eliminando..." : "Eliminar" }}
          </button>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.tipos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}

@media (max-width: 768px) {
  .tipos-grid {
    grid-template-columns: 1fr;
  }
}

.tipo-card {
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
  padding: 0.4rem 0.6rem;
  font-size: 0.85rem;
  white-space: nowrap;
  height: auto;
  display: inline-flex;
  align-items: center;
}
</style>
