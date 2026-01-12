<script setup lang="ts">
// Interfaz basada en ReporteDesempenoDronDTO
interface ReporteDTO {
  idDron: number;
  modelo: string;
  misionesCompletadas: number;
  misionesFallidas: number;
  horasVueloTotal: number;
}

// Llamada al backend
const { data: reporte, pending, error } = await useFetch<ReporteDTO[]>('/api/drones/reportes/reporte-global');
</script>

<template>
  <main class="container">
    <div class="headings">
      <div>
        <h2>Reporte Global de Desempeño</h2>
        <small class="text-muted">Resumen de actividad y fiabilidad por dron</small>
      </div>
      <NuxtLink to="/drones" role="button" class="secondary btn-compact">
        Volver a Drones
      </NuxtLink>
    </div>

    <article v-if="pending" aria-busy="true">
      Cargando reporte global...
    </article>

    <article v-else-if="error" class="error">
      No se pudo cargar el reporte. {{ error.message }}
    </article>

    <figure v-else>
      <table role="grid">
        <thead>
          <tr>
            <th scope="col">Dron</th>
            <th scope="col">Modelo</th>
            <th scope="col" style="text-align: center;">Completadas</th>
            <th scope="col" style="text-align: center;">Fallidas</th>
            <th scope="col" style="text-align: right;">Horas Totales</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(fila, index) in reporte" :key="index">
            <td>
              <strong>#{{ fila.idDron }}</strong>
            </td>
            <td>
              {{ fila.modelo }}
            </td>
            <td style="text-align: center;">
              <span class="text-positive">{{ fila.misionesCompletadas }}</span>
            </td>
            <td style="text-align: center;">
              <span :class="fila.misionesFallidas > 0 ? 'text-negative' : 'text-muted-light'">
                {{ fila.misionesFallidas }}
              </span>
            </td>
            <td style="text-align: right;">
              {{ fila.horasVueloTotal ? fila.horasVueloTotal.toFixed(2) : '0.00' }} h
            </td>
          </tr>
          
          <tr v-if="reporte && reporte.length === 0">
            <td colspan="5" style="text-align: center">
              No hay datos de desempeño registrados.
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
.text-muted {
  color: rgba(255, 255, 255, 0.6);
  display: block;
}
.text-muted-light {
  color: rgba(255, 255, 255, 0.3);
}

.text-positive { color: #22c55e; font-weight: bold; }
.text-negative { color: #ef4444; font-weight: bold; }

.btn-compact {
  padding: 0.4rem 0.8rem;
  font-size: 0.85rem;
  height: auto;
  display: inline-flex;
  align-items: center;
}
</style>