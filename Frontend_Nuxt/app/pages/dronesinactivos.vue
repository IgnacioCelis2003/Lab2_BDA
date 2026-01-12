<script setup lang="ts">
interface InactivosDTO {
  idDron: number
  nombreModelo: string
  estado: string
  ultimaMision: string
}
const { data, pending, error } = await useFetch<InactivosDTO[]>('/api/drones/reportes/inactivos');
</script>

<template>
  <div>

    <p v-if="pending">Cargando drones inactivos...</p>


    <p v-else-if="error">Error al cargar los datos</p>


    <table v-else class="tabla">
      <thead>
        <tr>
          <th>ID Dron</th>
          <th>Modelo</th>
          <th>Estado</th>
          <th>Última misión</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="dron in data" :key="dron.idDron">
          <td>{{ dron.idDron }}</td>
          <td>{{ dron.nombreModelo }}</td>
          <td>{{ dron.estado }}</td>
          <td>{{ dron.ultimaMision }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.tabla {
  width: 100%;
  border-collapse: collapse;
}

.tabla th,
.tabla td {
  border: 1px solid #060824;
  padding: 8px;
}

.tabla th {
  background-color: #02063d;
}
</style>
