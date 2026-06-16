<template><div class="page"><div class="page-head"><div><h2 class="page-title">学员管理</h2><p class="subtle">画像、阶段、风险等级和申请进度集中追踪。</p></div><el-button type="primary" @click="dialog=true">新增学员</el-button></div><StudentTable/><el-dialog v-model="dialog" title="新增学员" width="520"><el-form label-width="90px"><el-form-item label="姓名"><el-input /></el-form-item><el-form-item label="GPA"><el-input /></el-form-item><el-form-item label="目标院校"><el-input /></el-form-item></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="dialog=false">保存</el-button></template></el-dialog></div></template>
<script setup lang="ts">
import { ref } from 'vue';import StudentTable from '@/components/StudentTable.vue';const dialog=ref(false);
</script>
