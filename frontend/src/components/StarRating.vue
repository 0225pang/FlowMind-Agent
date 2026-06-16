<template>
  <span class="star-rating" :class="{ readonly }">
    <button
      v-for="star in 5"
      :key="star"
      class="star"
      :class="{ filled: star <= displayRating, interactive: !readonly }"
      :disabled="readonly"
      :title="readonly ? `${modelValue || 0} / 5 星` : `${star} 星`"
      type="button"
      @click="!readonly && select(star)"
      @mouseenter="!readonly && (hoverStar = star)"
      @mouseleave="!readonly && (hoverStar = 0)"
    >
      <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
        <path
          d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
          fill="currentColor"
        />
      </svg>
    </button>
    <small v-if="showLabel" class="star-label">{{ modelValue > 0 ? `${modelValue} 分` : '未评分' }}</small>
  </span>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: number
  readonly?: boolean
  showLabel?: boolean
}>(), {
  readonly: false,
  showLabel: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: number): void
}>()

const hoverStar = ref(0)
const displayRating = computed(() => hoverStar.value || props.modelValue || 0)

function select(star: number) {
  emit('update:modelValue', star)
}
</script>

<style scoped>
.star-rating {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.star {
  width: 22px;
  height: 22px;
  padding: 2px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #d1d5db;
  cursor: default;
  line-height: 0;
  transition: color 0.15s, background 0.15s;
}

.star.interactive {
  cursor: pointer;
}

.star.interactive:hover {
  color: #fbbf24;
  background: #fff7ed;
}

.star.filled {
  color: #f59e0b;
}

.star-label {
  margin-left: 6px;
  color: #667085;
  font-size: 12px;
}
</style>
