const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const dir=path.resolve(__dirname,'../reports/m002_a1_1'),r=JSON.parse(fs.readFileSync(path.join(dir,'RUNTIME.json'))),checks=[],gaps=[];
function check(test,pass,detail){checks.push({test,pass,detail});assert(pass,test+': '+JSON.stringify(detail));}
check('runtime_checks',r.checks.every(c=>c.pass)&&r.renderErrors===0,{passed:r.checks.filter(c=>c.pass).length,total:r.checks.length,renderErrors:r.renderErrors});
const expected={combo:['idle','sword','sword_b','sword_c','idle'],heavy:['idle','heavy','idle'],combo_jog:['idle','sword','sword_b','sword_c','jog'],pause:['idle','sword','idle','sword','idle'],cancel_a:['idle','sword','idle','roll','idle'],cancel_b:['idle','sword','sword_b','idle','roll','idle'],cancel_c:['idle','sword','sword_b','sword_c','idle','roll','idle'],cancel_heavy:['idle','heavy','idle','roll','idle']};
let markdown='# M002.A1.1 — Quadros críticos\n\nSequências reais em 20 amostras/s. Links antes/depois selecionados por mudança efetiva do clipe no renderer. A primeira imagem de um clipe pode já conter alguns milissegundos de progresso. Use VIEWER.html para reprodução contínua.\n\n| Cenário | Ângulo | Tempo | Troca | Antes | Depois |\n|---|---|---:|---|---|---|\n';
for(const s of r.series){
 const segments=[];let resets=0;
 for(let i=0;i<s.frames.length;i++){const f=s.frames[i],p=s.frames[i-1];
  if(!p||p.clip!==f.clip){segments.push({clip:f.clip.replace('UAL_',''),start:f.t});if(p)markdown+=`| ${s.name} | ${s.angle} | ${f.t.toFixed(2)} s | ${p.clip} → ${f.clip} | [PNG](${p.src}) | [PNG](${f.src}) |\n`;}
  else if(!['UAL_idle','UAL_jog'].includes(f.clip)&&f.clipTime+1e-4<p.clipTime)resets++;
 }
 check(s.name+'_'+s.angle+'_sequence',JSON.stringify(segments.map(s=>s.clip))===JSON.stringify(expected[s.name]),segments);
 check(s.name+'_'+s.angle+'_no_clip_restart',resets===0,{resets});
 if(s.name.startsWith('cancel')){const i=segments.findIndex(x=>x.clip==='roll');if(i>0&&segments[i-1].clip==='idle')gaps.push({scene:s.name,angle:s.angle,idleSeconds:segments[i].start-segments[i-1].start});}
}
fs.writeFileSync(path.join(dir,'PLAYBACK_AUDIT.json'),JSON.stringify({checks,cancellationIdleGaps:gaps,aestheticApproval:'PENDING_USER'},null,2));fs.writeFileSync(path.join(dir,'CRITICAL_FRAMES.md'),markdown);
console.log({passed:checks.filter(c=>c.pass).length,total:checks.length,cancellationIdleGaps:gaps});
