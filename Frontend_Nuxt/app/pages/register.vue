<template>
  <article class="login-card">
    
    <hgroup>
      <h3>Iniciar Sesión</h3>
      <p>Ingresa tus credenciales para continuar</p>
    </hgroup>

    <form @submit.prevent="submitForm">
      
        <label for="nombre">
        Nombre
        <input 
          type="text" 
          id="name" 
          name="name" 
          placeholder="Nombre" 
          v-model="form.nombre"
          required 
        />
      </label>

      <label for="email">
        Correo Electrónico
        <input 
          type="email" 
          id="email" 
          name="email" 
          placeholder="tu@correo.com" 
          v-model="form.email"
          required 
        />
      </label>

      <label for="password">
        Contraseña
        <input 
          type="password" 
          id="password" 
          name="password" 
          placeholder="Tu contraseña" 
          v-model="form.password"
          required 
        />
      </label>


      <button 
        type="submit" 
        class="contrast"
      >
        Registrar
      </button>
        <div v-if="error">{{ error }}</div>
        <div style="margin-top:1rem">
          <NuxtLink to="/login" class="secondary">Iniciar sesión</NuxtLink>
        </div>
    </form>

    
  </article>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'login',
})

const error = ref<string | null>(null);

async function submitForm(){
  error.value = null;
  try{
    await $fetch('/api/auth/register', {
      method: 'POST',
      body: form
    });
    // redirigir a login
    navigateTo('/login');
  }catch(e:any){
    error.value = e?.data?.message || e?.message || 'Error al registrar';
  }
}

const form = reactive({
  nombre: '',  
  email: '',
  password: ''
})

</script>

<style scoped>

.login-card {
  max-width: 480px; /* Limita el ancho del formulario */
  margin: 4rem auto; /* Centra el formulario vertical y horizontalmente */
  padding: 2rem;
}

button[aria-busy="true"] {
  padding-left: 3rem;
  padding-right: 3rem;
}
</style>