<script lang="ts" setup>

</script>

<template>
    <main class="container">
        <h2>Panel</h2>

        <div class="panels-grid">
            <NuxtLink to="/drones" class="panel-card drone-card">
                <div class="panel-icon">🚁</div>
                <h3 style="color: white;">Drones</h3>
                <p style="font-size: 0.8rem; color: black;">Ver y gestionar los drones registrados.</p>
                <div class="panel-arrow">→</div>
            </NuxtLink>

            <NuxtLink to="/misiones" class="panel-card mission-card">
                <div class="panel-icon">📋</div>
                <h3 style="color: white;">Misiones</h3>
                <p style="font-size: 0.8rem; color: black;">Listado de misiones y su estado.</p>
                <div class="panel-arrow">→</div>
            </NuxtLink>

            <NuxtLink to="/telemetria" class="panel-card summary-card">
                <div class="panel-icon">📊</div>
                <h3 style="color: white;">Estadísticas</h3>
                <p style="font-size: 0.8rem; color: black;">Estadísticas y resúmenes por tipo de misión.</p>
                <div class="panel-arrow">→</div>
            </NuxtLink>
        </div>

        <div class="secondary-grid">
            <NuxtLink to="/Lab2/puntosInteres" class="secondary-card interest-card">
                <div class="secondary-icon">📍</div>
                <div class="secondary-content">
                    <h3 style="color: white;">Puntos de Interés</h3>
                    <p style="font-size: 0.8rem; color: black;">Gestionar puntos de interés en el mapa.</p>
                </div>
                <div class="panel-arrow">→</div>
            </NuxtLink>

            <NuxtLink to="/Lab2/zonasProhibidas" class="secondary-card restricted-card">
                <div class="secondary-icon">🚫</div>
                <div class="secondary-content">
                    <h3 style="color: white;">Zonas Prohibidas</h3>
                    <p style="font-size: 0.8rem; color: black;">Definir y visualizar zonas restringidas.</p>
                </div>
                <div class="panel-arrow">→</div>
            </NuxtLink>
        </div>

            <!-- Imagen centrada debajo del grid -->
            <div style="display:flex;flex-direction:column;justify-content:center;align-items:center;margin-top:1.5rem;width:100%;gap:1rem;">
                <h1 style="margin:0;">Monitoreo en tiempo real</h1>
                <DroneMap />
            </div>

        </main>
</template>

<style scoped>
.panels-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 2rem;
    justify-content: center;
    align-items: stretch;
    max-width: 1000px;
    margin: 2rem auto;
    padding: 0 1rem;
}

.panel-card {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 2.5rem 1.5rem;
    border-radius: 12px;
    text-decoration: none;
    color: white;
    font-weight: 500;
    transition: all 0.3s ease;
    overflow: hidden;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
    border: 2px solid transparent;
    aspect-ratio: 1 / 1;
}

.panel-card::before {
    content: '';
    position: absolute;
    inset: 0;
    opacity: 0;
    transition: opacity 0.3s ease;
    z-index: -1;
}

.panel-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 25px rgba(0, 0, 0, 0.2);
}

.drone-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.drone-card:hover {
    border-color: rgba(255, 255, 255, 0.3);
}

.mission-card {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.mission-card:hover {
    border-color: rgba(255, 255, 255, 0.3);
}

.summary-card {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.summary-card:hover {
    border-color: rgba(255, 255, 255, 0.3);
}

.panel-icon {
    font-size: 3rem;
    position: justify;
    margin-top: 0.2rem;
    transition: transform 0.3s ease;
}

.panel-card:hover .panel-icon {
    transform: scale(1.15);
}

.panel-card h3 {
    font-size: 1.5rem;
    margin: 0.5rem 0;
    font-weight: 700;
    text-align: center;
}

.panel-card p {
    font-size: 0.95rem;
    margin: 0.75rem 0 1.5rem 0;
    text-align: center;
    opacity: 0.95;
    line-height: 1.4;
}

.panel-arrow {
    font-size: 2rem;

    opacity: 0;
    transform: translateX(-5px);
    transition: all 0.3s ease;
}

.panel-card:hover .panel-arrow {
    opacity: 1;
    transform: translateX(5px);
}

.secondary-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
    gap: 2rem;
    justify-content: center;
    align-items: stretch;
    max-width: 1000px;
    margin: 3rem auto;
    padding: 0 1rem;
}

.secondary-card {
    position: relative;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    padding: 1.5rem 2rem;
    border-radius: 12px;
    text-decoration: none;
    color: white;
    font-weight: 500;
    transition: all 0.3s ease;
    overflow: hidden;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
    border: 2px solid transparent;
    gap: 1.5rem;
}

.secondary-content {
    display: flex;
    flex-direction: column;
    justify-content: center;
    flex: 1;
}

.secondary-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 25px rgba(0, 0, 0, 0.2);
    border-color: rgba(255, 255, 255, 0.3);
}

.interest-card {
    background: linear-gradient(135deg, #fd6e6a 0%, #f46a3b 100%);
}

.restricted-card {
    background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.secondary-icon {
    font-size: 2.5rem;
    transition: transform 0.3s ease;
    flex-shrink: 0;
}

.secondary-card:hover .secondary-icon {
    transform: scale(1.15);
}

.secondary-card h3 {
    font-size: 1.2rem;
    margin: 0 0 0.3rem 0;
    font-weight: 700;
    text-align: left;
}

.secondary-card p {
    font-size: 0.9rem;
    margin: 0;
    text-align: left;
    opacity: 0.95;
    line-height: 1.3;
}

@media (max-width: 768px) {
    .panels-grid {
        grid-template-columns: 1fr;
        gap: 1.5rem;
        padding: 0 0.5rem;
    }

    .panel-card {
        padding: 2rem 1rem;
    }

    .panel-icon {

        font-size: 2rem;
    }

    .panel-card h3 {
        font-size: 1.3rem;
    }
}
</style>