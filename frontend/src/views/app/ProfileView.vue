<script setup lang="ts">
import { storeToRefs } from 'pinia'

import LanguageCard from '@/components/profile/LanguageCard.vue'
import ProfileDetailsCard from '@/components/profile/ProfileDetailsCard.vue'
import SignInMethodsCard from '@/components/profile/SignInMethodsCard.vue'
import { useAuthStore } from '@/stores/auth'

const { user } = storeToRefs(useAuthStore())
</script>

<template>
  <!-- Design P1 profile: two columns on desktop, one on mobile. Account deletion arrives in Phase 8. -->
  <div v-if="user" class="grid items-start gap-6 lg:grid-cols-2">
    <div class="flex flex-col gap-6">
      <ProfileDetailsCard :user="user" />
    </div>
    <div class="flex flex-col gap-6">
      <LanguageCard />
      <SignInMethodsCard :user="user" />
    </div>
  </div>
</template>
