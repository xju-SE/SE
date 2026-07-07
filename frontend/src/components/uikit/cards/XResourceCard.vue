<script setup lang="ts">
import pdfIcon from '../../../assets/icons/content/pdf.svg'
import codeIcon from '../../../assets/icons/content/code.svg'
withDefaults(defineProps<{title:string;kind?:'pdf'|'code';author?:string;downloads?:number;status?:'review'|'approved'|'rejected';image?:string}>(),{kind:'pdf',status:'approved'})
</script><template><article class="xj-card study"><div v-if="image"><img :src="image" class="xj-thumb" style="height:108px"/></div><div class="xj-resource"><img :src="kind==='pdf'?pdfIcon:codeIcon" class="xj-resource-icon"/><div class="xj-resource-main"><span class="xj-badge" :class="status==='approved'?'success':status==='review'?'warning':'danger'">{{status==='approved'?'已通过':status==='review'?'审核中':'已拒绝'}}</span><div class="xj-resource-title" style="margin-top:7px">{{title}}</div><div class="xj-resource-sub">{{author}} · ↓ {{downloads||0}}</div></div></div></article></template>
