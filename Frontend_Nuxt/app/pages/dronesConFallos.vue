<script setup lang="ts">
// Definimos la estructura de los datos que vienen del backend
interface DronFalloDTO {
  idDron: bigint;
  totalMisionesFallidas: bigint;
}

// Llamamos a nuestro proxy de Nuxt
const { 
  data: reporte, 
  pending, 
  error 
} = await useFetch<DronFalloDTO[]>('/api/drones/reportes/drones-con-fallos');
</script>

<template>
  <main class="container">
    <div class="headings">
      <h2>Los 5 Drones con más Fallos</h2>
      <NuxtLink to="/drones" type="button" class="secondary btn-compact">
        Volver a Drones
      </NuxtLink>
    </div>

    <article v-if="pending" aria-busy="true">
      Cargando reporte de fallos...
    </article>

    <article v-else-if="error" class="error">
      No se pudo cargar el reporte. {{ error.message }}
    </article>

    <figure v-else>
      <table role="grid">
        <thead>
          <tr>
            <th scope="col">ID Dron</th>
            <th scope="col">Cantidad de Fallos</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in reporte" :key="index">
            <td>
              <strong>#{{ item.idDron }}</strong>
            </td>
            <td style="color: #e53935; font-weight: bold;">
              {{ item.totalMisionesFallidas }}
            </td>
          </tr>
          
          <tr v-if="reporte && reporte.length === 0">
            <td colspan="3" style="text-align: center">
              ¡Excelente! No se han registrado misiones fallidas recientemente.
            </td>
          </tr>
        </tbody>
      </table>
    </figure>
  </main>
</template>

<style scoped>
.headings {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}
.error {
  background-color: #ffcdd2;
  color: #c62828;
  padding: 1rem;
  border-radius: 4px;
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