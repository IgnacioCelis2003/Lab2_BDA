<script setup lang="ts">
import { computed } from 'vue';

// Definimos la interfaz basada en el DTO del backend
interface DesempenoDTO {
  nombreTipoMision: string;
  nombreModelo: string;
  totalMisionesCompletadas: number;
}

// Llamada al backend
const { data: reporte, pending, error } = await useFetch<DesempenoDTO[]>('/api/misiones/reportes/desempeno-tipo-mision');

// Agrupamiento y procesamiento de datos por tipo de misión
const reporteAgrupadoPorTipo = computed(() => {
  if (!reporte.value) return [];

  // 1. Agrupar por nombre de tipo de misión
  const grupos: Record<string, { tipo: string; totalGeneral: number; detalles: DesempenoDTO[] }> = {};

  reporte.value.forEach((item) => {
    // Usamos el tipo de misión como clave
    if (!grupos[item.nombreTipoMision]) {
      grupos[item.nombreTipoMision] = {
        tipo: item.nombreTipoMision,
        totalGeneral: 0,
        detalles: []
      };
    }
    // Agregamos el ítem al grupo
    grupos[item.nombreTipoMision].detalles.push(item);
    
    // Sumamos al total acumulado de este tipo
    grupos[item.nombreTipoMision].totalGeneral += item.totalMisionesCompletadas;
  });

  // 2. Ordenar para que el tipo con más misiones salga primero
  return Object.values(grupos).sort((a, b) => b.totalGeneral - a.totalGeneral);
});
</script>

<template>
  <main class="container">
    <div class="headings">
      <h2>Desempeño por Tipo de Misión de los 2 Modelos más Usados</h2>
      <NuxtLink to="/misiones" role="button" class="secondary">
        Volver a Misiones
      </NuxtLink>
    </div>

    <article v-if="pending" aria-busy="true">
      Generando reporte...
    </article>

    <article v-else-if="error" class="error">
      Error cargando el reporte: {{ error.message }}
    </article>

    <div v-else>
      <div v-if="reporteAgrupadoPorTipo.length === 0" style="text-align: center">
        No hay datos para mostrar.
      </div>

      <article v-for="grupo in reporteAgrupadoPorTipo" :key="grupo.tipo" class="grupo-card">
        <header class="grupo-header">
          <div>
            <h3 style="margin-bottom: 0">{{ grupo.tipo }}</h3>
          </div>
          <div class="badge-total">
            <span style="font-size: 0.8rem">Total Completadas</span>
            <strong>{{ grupo.totalGeneral }}</strong>
          </div>
        </header>

        <table role="grid" class="tabla-detalles">
          <thead>
            <tr>
              <th scope="col">Modelo de Dron</th>
              <th scope="col" style="text-align: right">Completadas</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(fila, idx) in grupo.detalles" :key="idx">
              <td>{{ fila.nombreModelo }}</td>
              <td style="text-align: right">{{ fila.totalMisionesCompletadas }}</td>
            </tr>
          </tbody>
        </table>
      </article>
    </div>
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

/* Estilos para las tarjetas */
.grupo-card {
  margin-bottom: 2rem;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.grupo-header {
  background-color: rgba(255, 255, 255, 0.05);
  padding: 1rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.badge-total {
  background-color: var(--primary);
  color: var(--primary-inverse);
  padding: 0.5rem 1rem;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.2;
}

.tabla-detalles {
  margin: 0;
}
.tabla-detalles th,
.tabla-detalles td {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}
.text-muted {
  color: rgba(255, 255, 255, 0.6);
}
</style>