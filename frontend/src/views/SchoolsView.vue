<template><div class="page"><div class="page-head"><div><h2 class="page-title">院校情报</h2><p class="subtle">夏令营、预推免、报名条件和材料要求。</p></div><el-button type="primary">AI 匹配推荐</el-button></div><div class="grid three"><SchoolProjectCard v-for="p in projects" :key="p.id" :item="p" /></div></div></template>
<script setup lang="ts">
import SchoolProjectCard from '@/components/SchoolProjectCard.vue';import { projects } from '@/api/mock';
</script>
