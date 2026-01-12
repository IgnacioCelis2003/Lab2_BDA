<script lang="ts" setup>

</script>

<template>
    <main class="container">
        <h2>Panel</h2>

        <div class="grid" style="display:flex;gap:1rem;justify-content:center;flex-wrap:wrap;align-items:stretch">
            <NuxtLink to="/drones" class="card">
                <h3>Drones</h3>
                <p>Ver y gestionar los drones registrados.</p>
            </NuxtLink>

            <NuxtLink to="/misiones" class="card">
                <h3>Misiones</h3>
                <p>Listado de misiones y su estado.</p>
            </NuxtLink>

            <NuxtLink to="/telemetria" class="card">
                <h3>Resumen de Misiones</h3>
                <p>Estadísticas y resúmenes por tipo de misión.</p>
            </NuxtLink>
        </div>

            <!-- Imagen centrada debajo del grid -->
            <div style="display:flex;justify-content:center;margin-top:1.5rem;">
                <h1>
                     Monitoreo en tiempo real
                    <DroneMap />
                 </h1>
            </div>

        </main>
</template>