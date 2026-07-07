<script setup lang="ts">
import warningIcon from '../../../assets/icons/status/warning.svg'
import errorIcon from '../../../assets/icons/status/error.svg'
import successIcon from '../../../assets/icons/status/success.svg'
withDefaults(defineProps<{open:boolean;type?:'warning'|'danger'|'success';title:string;desc:string;confirmText?:string}>(),{type:'warning',confirmText:'确认'});defineEmits<{confirm:[];cancel:[]}>()
const icons={warning:warningIcon,danger:errorIcon,success:successIcon}
</script><template><Teleport to="body"><div v-if="open" style="position:fixed;inset:0;background:rgba(8,20,38,.4);display:grid;place-items:center;z-index:999" @click.self="$emit('cancel')"><section class="xj-modal"><div class="xj-modal-head"><img :src="icons[type]"/><div><div class="xj-modal-title">{{title}}</div><div class="xj-modal-desc">{{desc}}</div></div></div><div class="xj-modal-actions"><button class="xj-btn secondary sm" @click="$emit('cancel')">取消</button><button class="xj-btn sm" :class="type==='danger'?'danger':'life'" @click="$emit('confirm')">{{confirmText}}</button></div></section></div></Teleport></template>
