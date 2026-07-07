<script setup lang="ts">
import successIcon from '../../../assets/icons/status/success.svg'
import infoIcon from '../../../assets/icons/status/info.svg'
import warningIcon from '../../../assets/icons/status/warning.svg'
import errorIcon from '../../../assets/icons/status/error.svg'
withDefaults(defineProps<{type?:'success'|'info'|'warning'|'danger';title:string;desc?:string}>(),{type:'success'})
const icons={success:successIcon,info:infoIcon,warning:warningIcon,danger:errorIcon}
</script><template><div class="xj-toast" :class="type"><img :src="icons[type]" class="xj-toast-icon"/><div><div class="xj-toast-title">{{title}}</div><div v-if="desc" class="xj-toast-desc">{{desc}}</div></div></div></template>
