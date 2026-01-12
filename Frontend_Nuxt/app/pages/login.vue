<template>
  <article class="login-card">
    
    <hgroup>
      <h3>Iniciar Sesión</h3>
      <p>Ingresa tus credenciales para continuar</p>
    </hgroup>

    <form @submit.prevent="submitForm">
      
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
        Ingresar
      </button>
        <div v-if="error">{{ error }}</div>
        <div style="margin-top:1rem">
          <NuxtLink to="/register" class="secondary">¿No tienes cuenta? Regístrate</NuxtLink>
        </div>
    </form>

    
  </article>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'login',
})

const { login, error: authError, loading } = useAuth();

const error = authError;

async function submitForm(){
  error.value = null;
  if(!form.email){
    error.value = "Debes ingresar un correo"
    return;
  }

  try{
    await login(form.email, form.password);
    // Redirigir a la página principal
    navigateTo('/');
  }catch(e){
    // error ya está en el composable
  }
}

const form = reactive({
  email: '',
  password: ''
})

</script>

<style scoped>
/* Aunque Pico hace la mayoría del trabajo, podemos añadir estilos 
  específicos para centrar o limitar el ancho de nuestro componente.
*/
.login-card {
  max-width: 480px; /* Limita el ancho del formulario */
  margin: 4rem auto; /* Centra el formulario vertical y horizontalmente */
  padding: 2rem;
}

/* El spinner de Pico es un poco grande, lo ajustamos */
button[aria-busy="true"] {
  padding-left: 3rem;
  padding-right: 3rem;
}
</style>