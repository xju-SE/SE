<script setup lang="ts">defineProps<{src:string;size?:'sm'|'md'|'lg';alt?:string}>()</script><template><img :src="src" :alt="alt||''" class="xj-icon" :class="size||'md'" /></template>
