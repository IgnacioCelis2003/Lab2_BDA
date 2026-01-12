<template>
  <div>
    <h2>Buscar Drones Cercanos</h2>

    <!-- Formulario para ingresar coordenadas -->
    <form @submit.prevent="buscarDrones">
      <label>
        Latitud:
        <input v-model.number="lat" type="number" step="0.000001" required />
      </label>
      <label>
        Longitud:
        <input v-model.number="lon" type="number" step="0.000001" required />
      </label>
      <button type="submit">Buscar</button>
    </form>

    <!-- Tabla de resultados -->
    <table v-if="drones.length">
      <thead>
        <tr>
          <th>ID del Dron</th>
          <th>Distancia Mínima (m)</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in drones" :key="d.idDron">
          <td>{{ d.idDron }}</td>
          <td>{{ d.distanciaMinimaMetros.toFixed(2) }}</td>
        </tr>
      </tbody>
    </table>

    <p v-else-if="buscado">No se encontraron drones cercanos.</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const lat = ref(null)
const lon = ref(null)
const drones = ref([])
const buscado = ref(false)

async function buscarDrones() {
  if (lat.value == null || lon.value == null) return

  try {
    const { data } = await $fetch(`/api/cercanos/${lat.value}/${lon.value}`)
    drones.value = data
    buscado.value = true
  } catch (error) {
    console.error('Error al obtener drones:', error)
    drones.value = []
    buscado.value = true
  }
}
</script>

<style scoped>
form {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
label {
  display: flex;
  flex-direction: column;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th, td {
  border: 1px solid #ccc;
  padding: 8px;
  text-align: center;
}
button {
  padding: 5px 10px;
}
</style>
