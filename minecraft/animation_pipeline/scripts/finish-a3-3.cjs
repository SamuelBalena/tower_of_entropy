const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'animation_pipeline/reports/m002_a3_3');
const read=p=>JSON.parse(fs.readFileSync(path.join(out,p),'utf8').replace(/^\uFEFF/,''));
const suites={};
for(const [name,file]of Object.entries({refinements:'REFINEMENT_RUNTIME.json',combat:'regression_a1/RUNTIME.json',iframes:'IFRAMES.json',vanilla:'vanilla/RUNTIME.json',coverage:'coverage/RUNTIME.json',worlds:'global/GLOBAL_RUNTIME.json'})){
 const r=read(file);assert(r.checks.length>0);assert.deepEqual(r.checks.filter(c=>!c.pass),[],name);
 suites[name]={pass:r.checks.length,total:r.checks.length,file};
}
const runtime=read('REFINEMENT_RUNTIME.json'),rows=n=>runtime.frames.filter(f=>f.case===n),at=(n,p)=>{const f=rows(n).find(f=>f.phase===p);assert(f,n+' '+p);return f;};
const dist=(a,b)=>Math.hypot(a.x-b.x,a.z-b.z);
const distances={A:dist(at('combo',30),at('combo',40)),B:dist(at('combo',40),at('combo',50)),C:dist(at('combo',50),at('combo',72)),heavy:dist(at('heavy',30),at('heavy',72)),combo:dist(at('combo',30),at('combo',72))};
for(const [n,d]of Object.entries({A:.4,B:.5,C:.6,heavy:.7,combo:1.5}))assert(Math.abs(distances[n]-d)<.005,n+' distance');
const heavyRelease=runtime.checks.filter(c=>c.test.endsWith('_recovery')).map(c=>({test:c.test,ticks:+c.detail.match(/age=(\d+)/)[1]}));assert(heavyRelease.length>=2&&heavyRelease.every(c=>c.ticks===35));
assert(rows('heavy').some(f=>f.phase>=68&&!f.lock&&f.clip==='UAL_idle'));
assert(rows('heavy_sprint').some(f=>f.phase>=68&&f.phase<=75&&!f.lock&&f.clip==='UAL_sprint'));
const artifact=read('ARTIFACT.json'),preservation=read('PRESERVATION_AND_KEYFRAMES.json');assert.equal(artifact.status,'PASS');assert.equal(preservation.status,'PASS');
const builds={};for(const kind of ['normal','r31']){const b=fs.readFileSync(path.join(root,'.logs/m002-a3-3-build-'+kind+'.log')),t=b.toString(b[0]===255&&b[1]===254?'utf16le':'utf8');assert(t.includes('BUILD SUCCESSFUL')&&!t.includes('BUILD FAILED'),kind);builds[kind]=(t.match(/BUILD SUCCESSFUL in ([^\r\n]+)/)||[])[1];}
const result={status:'TECHNICAL_PASS_VISUAL_APPROVAL_REQUIRED',suites,totalChecks:Object.values(suites).reduce((n,s)=>n+s.total,0),distances,heavy:{releaseTicks:35,seconds:1.75,impactTick:12,unchangedPlaybackSeconds:1.5,recoverySeconds:.25,measuredReleases:heavyRelease,returnsToIdleAndSprint:true},builds,artifact,preservation,notCertified:['Dedicated server','Remote/artificial latency','Prolonged frame stalls','All external mod forces','Final aesthetic approval','R3.1 visual runtime']};
fs.writeFileSync(path.join(out,'RESULTS.json'),JSON.stringify(result,null,2));
const review=path.join(out,'REVIEW');fs.mkdirSync(review,{recursive:true});
let gallery='# M002.A3.3 — seleção de capturas reais\n\nCapturas do cliente Forge durante ações autorizadas no servidor integrado. Ângulo frontal; a câmera acompanha o jogador. Não é vídeo contínuo, nem comparação de performance. Fase 30 é a entrada do pedido; autorização chega após transporte local. As fases seguintes são ticks de cliente a 20 Hz.\n\nO trecho pesado 60–70 cobre o fim do giro, recuperação acelerada e retorno a Idle. O tempo do clipe é remapeado; os números de fase não são quadros da animação fonte. Avaliação estética permanece com o usuário.\n\n';
for(const n of ['heavy-60','heavy-62','heavy-64','heavy-66','heavy-70','heavy_sprint-70','combo-64','edge-60','slim-38']){const src=path.join(out,'screenshots',n+'.png');assert(fs.existsSync(src));fs.copyFileSync(src,path.join(review,n+'.png'));gallery+='## '+n+'\n\n!['+n+']('+n+'.png)\n\n';}fs.writeFileSync(path.join(review,'README.md'),gallery);
const log=path.join(root,'docs/TEST_LOG_M002_A3_3.md');let md=fs.readFileSync(log,'utf8').split('<!-- RESULTS -->')[0];md+='<!-- RESULTS -->\n\n## Resultados finais\n\n| Bateria | Resultado |\n|---|---:|\n';
for(const [name,s]of Object.entries(suites))md+='| '+name+' | '+s.pass+'/'+s.total+' |\n';
md+='| Build normal | passou — '+builds.normal+' |\n| Build R3.1 | passou — '+builds.r31+' |\n| Auditoria de preservação e JAR | passou |\n\n';
md+='**'+result.totalChecks+' verificações finais aprovadas**, além das duas compilações e auditorias. Distâncias reais, por diferença de posições a cada tick: A **'+distances.A.toFixed(3)+'**, B **'+distances.B.toFixed(3)+'**, C **'+distances.C.toFixed(3)+'**, pesado **'+distances.heavy.toFixed(3)+'**, combo **'+distances.combo.toFixed(3)+' bloco**. Arredondamento exibido a 0,001; os JSONs preservam os valores completos.\n\n';
md+='Pesado: todas as '+heavyRelease.length+' verificações de recuperação registraram **35 ticks / 1,75 s**, com impacto único no tick 12. As amostras após o fim retornam a Idle parado e a Sprint quando mantidos W/Sprint. A redução de controle em relação aos 47 ticks anteriores é **600 ms**. O trecho final visual dura **250 ms**.\n\n';
for(const key of ['wall_no_horizontal_reconcile','edge_no_horizontal_reconcile','cancel_a_roll','cancel_heavy_roll','hold_jump_no_buffer','remap_jump_no_buffer','autojump_blocked','server_jump_rejected_without_cancel']){for(const c of runtime.checks.filter(c=>c.test===key))md+='- `'+c.test+'`: '+c.detail+'\n';}
md+='\nI-frames, dano bruto leve **4**, pesado **6**, combo **12 HP** e impactos únicos passaram nos cenários controlados. A prova de dano usa alvo alinhado e sem knockback para isolar o cálculo; não implica acerto automático em alvos móveis ou fora do arco.\n\n';
md+='JAR normal: `build/libs/towerofentropy-0.0.1.jar`, **'+artifact.bytes+' bytes**, '+artifact.entries+' entradas, SHA-256 `'+artifact.sha256+'`. Sem classes de QA ou R3.1 no artefato normal. O experimental está separado em `.downloads/m002_a3_3/experimental/`.\n\n';
md+='[Resultados estruturados](../animation_pipeline/reports/m002_a3_3/RESULTS.json) e [capturas selecionadas](../animation_pipeline/reports/m002_a3_3/REVIEW/README.md). Conclusão técnica aprovada nos cenários medidos; homologações e aprovação estética listadas acima continuam pendentes. Nenhum próximo marco iniciado.\n';fs.writeFileSync(log,md);
console.log(JSON.stringify({status:result.status,suites,totalChecks:result.totalChecks,distances,builds},null,2));
