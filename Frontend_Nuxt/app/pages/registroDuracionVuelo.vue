<script setup lang="ts">
// Definimos la estructura de los datos que vienen del backend
interface DuracionVueloDTO {
  nombreModelo: string;
  tiempoTotalHoras: number;
}

// Llamamos a nuestro proxy de Nuxt
const { 
  data: reporte, 
  pending, 
  error 
} = await useFetch<DuracionVueloDTO[]>('/api/drones/reportes/duracion-vuelo');
</script>

<template>
  <main class="container">
    <div class="headings">
      <h2>Duración de Vuelo (Último Mes)</h2>
      <NuxtLink to="/modelosdron" type="button" class="secondary btn-compact">
        Volver a Modelos
      </NuxtLink>
    </div>

    <article v-if="pending" aria-busy="true">
      Cargando reporte...
    </article>

    <article v-else-if="error" class="error">
      No se pudo cargar el reporte. {{ error.message }}
    </article>

    <figure v-else>
      <table role="grid">
        <thead>
          <tr>
            <th scope="col">Modelo de Dron</th>
            <th scope="col">Horas Totales de Vuelo</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in reporte" :key="index">
            <td>
              <strong>{{ item.nombreModelo }}</strong>
            </td>
            <td>
              {{ item.tiempoTotalHoras.toFixed(2) }} hrs
            </td>
          </tr>
          <tr v-if="reporte && reporte.length === 0">
            <td colspan="2" style="text-align: center">
              No hay vuelos registrados en el último mes.
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