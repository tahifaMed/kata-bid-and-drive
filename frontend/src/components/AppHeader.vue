<script setup lang="ts">
import { RouterLink } from 'vue-router'
import UserSwitcher from './UserSwitcher.vue'
</script>

<template>
  <header class="border-b border-border">
    <div class="container-app flex flex-wrap items-center gap-x-6 gap-y-3 py-4">
      <RouterLink to="/" class="text-lg font-bold no-underline">BidAndDrive</RouterLink>

      <nav class="flex flex-wrap items-center gap-x-4 gap-y-2" aria-label="Main">
        <RouterLink to="/auctions" class="no-underline">Auctions</RouterLink>
        <RouterLink to="/me/bids" class="no-underline">My bids</RouterLink>
        <span class="text-ink-muted" aria-disabled="true">Watch list</span>
        <span class="text-ink-muted" aria-disabled="true">About</span>
      </nav>

      <div class="flex w-full flex-wrap items-center gap-3 sm:ml-auto sm:w-auto">
        <slot name="search" />
        <UserSwitcher />
        <slot />
      </div>
    </div>
  </header>
</template>
