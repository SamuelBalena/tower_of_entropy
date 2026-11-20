const fs=require('fs'),path=require('path'),assert=require('assert/strict');const dir=path.resolve(__dirname,'../reports/m002_a1_2');
const series=[];let pngs=0;
for(const variant of ['before','after']){const r=JSON.parse(fs.readFileSync(path.join(dir,variant,'RUNTIME.json')));for(const s of r.series){
 const first=s.frames.find(f=>/^UAL_(sword|heavy)/.test(f.clip));const shift=first?first.t-first.clipTime-.5:0;
 const frames=s.frames.map(f=>{assert(fs.existsSync(path.join(dir,variant,f.src)),f.src);pngs++;return {src:variant+'/'+f.src,t:f.t-shift,captureT:f.t,clip:f.clip,clipTime:f.clipTime,tick:f.tick,serverImpacts:f.combat?.impacts||0,blend:f.visual.blend,ground:f.visual.groundBlocks,sole:f.visual.soleBlocks};});
 series.push({variant,name:s.name,angle:s.angle,frames});
}}
let html=fs.readFileSync(path.join(__dirname,'a11-viewer.html'),'utf8');
function replace(a,b){assert(html.includes(a),'Template marker missing '+a);html=html.replace(a,b);}
replace('<title>Tower of Entropy · M002.A1.1</title>','<title>Tower of Entropy · M002.A1.2 · Antes/depois</title>');
replace('<h1>Tower of Entropy — ataques e transições</h1>','<h1>Tower of Entropy — A1.1 × A1.2</h1>');
replace('Capturas reais do cliente Forge com Polymesh R3. Compare movimentos e ângulos no mesmo relógio.','Antes e depois reais no cliente Forge/Polymesh R3. Mesmos roteiros, ângulos e iluminação. Início do primeiro golpe alinhado pelo relógio capturado; resolução temporal de 50 ms.');
replace('<select aria-label="Ângulo"></select></div>','<select aria-label="Ângulo"></select><select aria-label="Versão"></select></div>');
replace("selects[1].value=i?'side':'front';","selects[1].value='front';selects[0].value='heavy';selects[2].add(new Option('Antes · A1.1','before'));selects[2].add(new Option('Depois · A1.2','after'));selects[2].value=i?'after':'before';");
replace('angle:selects[1],img:','angle:selects[1],variant:selects[2],img:');
replace("s.angle===p.angle.value);","s.angle===p.angle.value&&s.variant===p.variant.value);");
replace("cancel_heavy:'Pesado cancelado por Roll'","cancel_heavy:'Pesado cancelado por Roll',jog_heavy:'Jog → pesado',heavy_jog:'Pesado → Jog'");
replace("+' · tentativas de impacto '+frame.serverImpacts;","+' · impactos '+frame.serverImpacts+' · blend '+Number(frame.blend).toFixed(2)+' · apoio Y '+Number(frame.sole).toFixed(3)+' bloco';");
replace('Marcas indicam mudanças de clipe e, quando presentes na telemetria, impactos do servidor.','Marcas indicam mudanças de clipe e tentativas de impacto do servidor. O blend e a altura visual de apoio aparecem na legenda. A prévia do Roll não antecipa a física nem os i-frames.');
replace('mantenha VIEWER.html e a pasta screenshots juntos.','mantenha COMPARATOR.html e as pastas before/after com screenshots juntas.');
replace('__DATA__',JSON.stringify(series).replaceAll('<','\\u003c'));
fs.writeFileSync(path.join(dir,'COMPARATOR.html'),html);
fs.writeFileSync(path.join(dir,'EVIDENCE.json'),JSON.stringify({series:series.length,pngs,fps:20,variants:['before','after'],scenarios:10,angles:['front','side','back'],alignment:'First attack source-clock origin aligned; frame selection accuracy limited to 50 ms capture steps',source:'Real Minecraft screenshots; baseline uses preserved legacy path; no generated in-between frames'},null,2));
console.log({series:series.length,pngs,htmlBytes:Buffer.byteLength(html)});
