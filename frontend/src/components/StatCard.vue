<template><div class="stat surface hoverable"><div class="icon" :style="{background: color}"><el-icon><component :is="icon" /></el-icon></div><div><span>{{ label }}</span><strong>{{ value }}</strong><small>{{ trend }}</small></div></div></template>
<script setup lang="ts">



defineProps<{label:string,value:string|number,trend:string,icon:string,color:string}>();
</script>
<style scoped>.stat{padding:18px;display:flex;align-items:center;gap:14px}.icon{width:44px;height:44px;border-radius:8px;color:white;display:grid;place-items:center}.stat span{color:#667085;font-size:13px}.stat strong{display:block;font-size:26px;margin:4px 0}.stat small{color:#19b37b}</style>


