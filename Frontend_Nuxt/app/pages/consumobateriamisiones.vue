<script setup lang="ts">
// Definimos la interfaz basada BateriaConsumoDTO
interface BateriaDTO {
  idMision: number;
  duracionMinutos: number;
  consumoBateria: number;
}

// Llamamos al proxy de Nuxt
const { data: reporte, 
    pending, 
    error 
} = await useFetch<BateriaDTO[]>('/api/misiones/reportes/consumo-bateria');
</script>

<template>
  <main class="container">
    <div class="headings">
      <div>
        <h2>Patrones de Consumo de Batería</h2>
        <small class="text-muted">
          Top 3 misiones largas con consumo inusualmente bajo
          (consumo menor al del 20% de las misiones más cortas)
        </small>
      </div>
      <NuxtLink to="/misiones" role="button" class="secondary">
        Volver a Misiones
      </NuxtLink>
    </div>

    <article v-if="pending" aria-busy="true">
      Detectando patrones de consumo...
    </article>

    <article v-else-if="error" class="error">
      Error cargando el reporte: {{ error.message }}
    </article>

    <figure v-else>
      <table role="grid">
        <thead>
          <tr>
            <th scope="col">ID Misión</th>
            <th scope="col">Duración (min)</th>
            <th scope="col">Consumo Batería (%)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(fila, index) in reporte" :key="index">
            <td>
              <strong>#{{ fila.idMision }}</strong>
            </td>
            <td>
              {{ fila.duracionMinutos.toFixed(2) }} min
            </td>
            <td>
              <span class="battery-tag">{{ fila.consumoBateria.toFixed(2) }}%</span>
            </td>
          </tr>
          
          <tr v-if="reporte && reporte.length === 0">
            <td colspan="4" style="text-align: center">
              No se detectaron misiones con este patrón de consumo anómalo.
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
.headings div {
  max-width: 70%;
}
.error {
  background-color: #ffcdd2;
  color: #c62828;
  padding: 1rem;
  border-radius: 4px;
}
.text-muted {
  color: rgba(255, 255, 255, 0.6);
  display: block;
  margin-top: 0.25rem;
}
.battery-tag {
  color: #22c55e;
  font-weight: bold;
}
</style>