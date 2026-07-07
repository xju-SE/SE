<script setup lang="ts">
import empty from '../../../assets/states/empty.svg'
import noResults from '../../../assets/states/no-results.svg'
import networkError from '../../../assets/states/network-error.svg'
import noPermission from '../../../assets/states/no-permission.svg'
import deleted from '../../../assets/states/deleted.svg'
import imageError from '../../../assets/states/image-error.svg'
import success from '../../../assets/states/success.svg'
import loading from '../../../assets/states/loading.svg'
withDefaults(defineProps<{type:'empty'|'no-results'|'network-error'|'no-permission'|'deleted'|'image-error'|'success'|'loading';title?:string;desc?:string;action?:string}>(),{})
const assets={empty,'no-results':noResults,'network-error':networkError,'no-permission':noPermission,deleted,'image-error':imageError,success,loading}
</script><template><section class="xj-state"><img :src="assets[type]" alt=""/><h4 v-if="title">{{title}}</h4><p v-if="desc">{{desc}}</p><button v-if="action" class="xj-btn secondary sm">{{action}}</button></section></template>
