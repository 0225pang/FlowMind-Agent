<template><div class="project surface hoverable"><div class="top"><strong>{{ item.schoolName }}</strong><el-tag type="warning">匹配 {{ item.matchScore }}</el-tag></div><h3>{{ item.projectName }}</h3><p><b>截止：</b>{{ item.deadline }}</p><p><b>条件：</b>{{ item.requirements }}</p><p><b>材料：</b>{{ item.materials }}</p><el-button type="primary" plain>AI 匹配推荐</el-button></div></template>
<script setup lang="ts">
defineProps<{item:any}>();
</script>
<style scoped>.project{padding:16px}.top{display:flex;align-items:center;justify-content:space-between}.project h3{font-size:16px;margin:12px 0}.project p{font-size:13px;color:#667085;line-height:1.6}</style>
