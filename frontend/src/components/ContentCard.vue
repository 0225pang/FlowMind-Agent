<template><div class="content-card surface hoverable"><div><el-tag size="small">{{ item.platform }}</el-tag><el-tag size="small" type="success">{{ item.status }}</el-tag></div><h3>{{ item.title }}</h3><p>{{ item.type }} · 热度 {{ item.heat }}</p></div></template>
<script setup lang="ts">
defineProps<{item:any}>();
</script>
<style scoped>.content-card{padding:16px}.content-card div{display:flex;gap:8px}.content-card h3{font-size:16px;margin:14px 0 8px}.content-card p{margin:0;color:#667085;font-size:13px}</style>
