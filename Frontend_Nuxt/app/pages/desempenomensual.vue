<script setup lang="ts">
// Definimos la interfaz basada el DTO
interface DesempenoMensualDTO {
  mes: string;                          // "YYYY-MM"
  promedioSemanal: number;
  diferenciaMesAnterior: number | null; // Puede ser null en el primer mes
}

// Llamamos al proxy
const { data: reporte, 
    pending, 
    error 
} = await useFetch<DesempenoMensualDTO[]>('/api/misiones/reportes/desempeno-mensual');

// Función auxiliar para formatear la diferencia (ej: "+5.20")
function formatDiff(val: number | null) {
  if (val === null || val === undefined) return '-';
  const signo = val > 0 ? '+' : '';
  return `${signo}${val.toFixed(2)}`;
}

// Clase dinámica para el color (verde si sube, rojo si baja)
function diffClass(val: number | null) {
  if (!val) return '';
  return val > 0 ? 'text-positive' : 'text-negative';
}
</script>

<template>
  <main class="container">
    <div class="headings">
      <div>
        <h2>Análisis de Desempeño Mensual</h2>
        <small class="text-muted">Promedio semanal de misiones completadas y variación vs. mes anterior</small>
      </div>
      <NuxtLink to="/misiones" role="button" class="secondary btn-compact">
        Volver
      </NuxtLink>
    </div>

    <article v-if="pending" aria-busy="true">
      Calculando promedios semanales...
    </article>

    <article v-else-if="error" class="error">
      Error cargando el reporte: {{ error.message }}
    </article>

    <figure v-else>
      <table role="grid">
        <thead>
          <tr>
            <th scope="col">Mes</th>
            <th scope="col">Promedio Semanal</th>
            <th scope="col">Variación (vs Mes Anterior)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(fila, index) in reporte" :key="index">
            <td>
              <strong>{{ fila.mes }}</strong>
            </td>
            <td>
              {{ fila.promedioSemanal.toFixed(2) }}
            </td>
            <td>
              <span :class="diffClass(fila.diferenciaMesAnterior)" style="font-weight: bold;">
                {{ formatDiff(fila.diferenciaMesAnterior) }}
              </span>
              <span v-if="fila.diferenciaMesAnterior !== null"> misiones</span>
              <span v-else class="text-muted" style="font-size: 0.8rem">(Sin datos previos)</span>
            </td>
          </tr>
          
          <tr v-if="reporte && reporte.length === 0">
            <td colspan="3" style="text-align: center">
              No hay datos históricos suficientes para el análisis.
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
  color: rgba(255, 255, 255, 0.5);
}

/* Colores para la variación */
.text-positive {
  color: #22c55e; /* Verde */
}
.text-negative {
  color: #ef4444; /* Rojo */
}

.btn-compact {
  padding: 0.4rem 0.8rem;
  font-size: 0.85rem;
  height: auto;
  display: inline-flex;
  align-items: center;
}
</style>