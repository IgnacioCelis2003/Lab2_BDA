<script lang="ts" setup>
interface ResumenMision {
  nombreTipo: string;
  cantidadTotal: number;
  promedioHoras: number;
}

const { data: resumen, pending, error } = await useFetch<ResumenMision[]>('/api/misiones/reportes/resumen-tipo');
</script>

<template>
    <main class="container">
        <div class="headings">
            <h2>Telemetría - Resumen de Misiones</h2>
            <NuxtLink to="/misiones" role="button" class="secondary">
                Volver a Misiones
            </NuxtLink>
        </div>
        <p>Aquí se muestran estadísticas y resúmenes por tipo de misión. Se hace uso de una vista materializada para optimizar las consultas.</p>

        <article v-if="pending" aria-busy="true">
            Generando reporte...
        </article>

        <article v-else-if="error" class="error">
            Error cargando el reporte: {{ error.message }}
        </article>

        <div v-else>
            <div v-if="!resumen || resumen.length === 0" style="text-align: center">
                No hay datos para mostrar.
            </div>

            <table v-else role="grid">
                <thead>
                    <tr>
                        <th scope="col">Tipo de Misión</th>
                        <th scope="col" style="text-align: right">Cantidad Total</th>
                        <th scope="col" style="text-align: right">Promedio Horas</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(fila, idx) in resumen" :key="idx">
                        <td>{{ fila.nombreTipo }}</td>
                        <td style="text-align: right">{{ fila.cantidadTotal }}</td>
                        <td style="text-align: right">{{ fila.promedioHoras.toFixed(2) }}</td>
                    </tr>
                </tbody>
            </table>
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

table {
    width: 100%;
    margin-top: 1rem;
}

table th,
table td {
    padding: 1rem;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

table th {
    background-color: rgba(255, 255, 255, 0.05);
    font-weight: 600;
}

table tbody tr:hover {
    background-color: rgba(255, 255, 255, 0.02);
}
</style>