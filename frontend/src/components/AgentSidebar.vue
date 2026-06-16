<template><div class="agent-list surface"><button v-for="agent in agents" :key="agent.type" :class="['agent', {active: modelValue===agent.type}]" @click="$emit('update:modelValue', agent.type)"><span class="dot" :style="{background:agent.color}"></span><strong>{{ agent.name }}</strong><small>{{ agent.desc }}</small></button></div></template>
<script setup lang="ts">



import { agents } from '@/api/mock';defineProps<{modelValue:string}>();defineEmits(['update:modelValue']);
</script>
<style scoped>.agent-list{padding:10px;display:flex;flex-direction:column;gap:8px}.agent{border:1px solid transparent;background:transparent;border-radius:8px;text-align:left;padding:12px;cursor:pointer;display:grid;grid-template-columns:auto 1fr;gap:2px 10px;transition:.18s}.agent:hover,.agent.active{background:#f4f6ff;border-color:#dfe4ff}.dot{width:10px;height:10px;border-radius:50%;margin-top:5px}.agent strong{font-size:14px}.agent small{grid-column:2;color:#667085}</style>


