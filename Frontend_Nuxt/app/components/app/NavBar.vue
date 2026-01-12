<template>
    <nav class="container">
  <ul>
    <li>
        <NuxtLink to="/">
        <strong>App Drones</strong>
        </NuxtLink>
    </li>
  </ul>
  <ul>
    <li v-if="authenticated">
      <button @click="doLogout" class="contrast">Logout</button>
    </li>
  </ul>
  </nav>
</template>

<script setup lang="ts">
const { data } = await useFetch('/api/auth/check');
const authenticated = computed(() => !!data.value?.authenticated);

async function doLogout(){
  await $fetch('/api/auth/logout', { method: 'POST' });
  // refresh page
  await navigateTo('/login');
}
</script>