<template><div class="chart-card surface"><div class="chart-head"><div><strong>{{ title }}</strong><span>{{ desc }}</span></div><el-button text :icon="Refresh">??</el-button></div><div ref="el" class="chart" /></div></template>
<script setup lang="ts">



import { onMounted, ref, watch } from 'vue';import * as echarts from 'echarts';import { Refresh } from '@element-plus/icons-vue';const props=defineProps<{title:string,desc:string,option:any}>();const el=ref<HTMLDivElement>();let chart:echarts.ECharts|undefined;function render(){if(!el.value)return;chart=chart||echarts.init(el.value);chart.setOption(props.option);chart.resize()}onMounted(()=>{render();window.addEventListener('resize',()=>chart?.resize())});watch(()=>props.option,render,{deep:true});
</script>
<style scoped>.chart-card{padding:16px}.chart-head{display:flex;justify-content:space-between;gap:10px;margin-bottom:8px}.chart-head strong{display:block}.chart-head span{color:#667085;font-size:12px}.chart{height:280px}</style>


