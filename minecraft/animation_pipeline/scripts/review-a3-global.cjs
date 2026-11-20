const fs=require('fs'),path=require('path');
const out=path.resolve(__dirname,'../reports/m002_a3_1'),read=p=>JSON.parse(fs.readFileSync(path.join(out,p),'utf8').replace(/^\uFEFF/,''));
const classic=read('coverage/RUNTIME.json'),slim=read('skincheck-slim/RUNTIME.json');
const data=slim.series.map(s=>({name:s.name,classic:classic.series.find(c=>c.name===s.name).frames.map(f=>'coverage/'+f.src),slim:s.frames.map(f=>'skincheck-slim/'+f.src),time:s.frames.map(f=>f.t)}));
for(const s of data)for(const p of [...s.classic,...s.slim])if(!fs.existsSync(path.join(out,p)))throw Error('Missing '+p);
fs.writeFileSync(path.join(out,'SKIN_COMPARISON.html'),`<!doctype html><meta charset="utf-8"><title>A3.1 Classic / Slim</title>
<style>body{font:16px system-ui;background:#18202c;color:#eee;margin:24px}img{width:100%}.pair{display:grid;grid-template-columns:1fr 1fr;gap:16px}label{display:block}button,select,input{margin:8px}input{width:45%}</style>
<h1>M002.A3.1 — Classic / Slim</h1><p>Capturas reais, frontal, 10 Hz. Comparação técnica; aprovação estética reservada ao usuário. O tempo indica a fase do cenário de QA, incluindo preparação e recuperação.</p>
<select id="scene"></select><button id="play">Reproduzir</button><select id="speed"><option value="1">1×</option><option value="0.5">0,5×</option><option value="0.25">0,25×</option></select><input id="frame" type="range" min="0" max="35" value="0"><span id="time"></span>
<div class="pair"><label>Classic — R3 aprovado<img id="classic"></label><label>Slim — braços 3 px / UVs derivados<img id="slim"></label></div>
<script>const data=${JSON.stringify(data).replace(/</g,'\\u003c')};const scene=document.querySelector('#scene'),frame=document.querySelector('#frame');let running=false,previous=0,elapsed=0;
data.forEach((s,i)=>scene.add(new Option(s.name,i)));function draw(){const s=data[scene.value||0],i=+frame.value;document.querySelector('#classic').src=s.classic[i];document.querySelector('#slim').src=s.slim[i];document.querySelector('#time').textContent=s.time[i].toFixed(2)+' s';}
scene.onchange=()=>{frame.value=0;draw()};frame.oninput=draw;document.querySelector('#play').onclick=()=>{running=!running;document.querySelector('#play').textContent=running?'Pausar':'Reproduzir'};
function tick(t){if(running){elapsed+=(t-previous)*+document.querySelector('#speed').value;if(elapsed>=100){frame.value=(+frame.value+Math.floor(elapsed/100))%36;elapsed%=100;draw()}}previous=t;requestAnimationFrame(tick)}draw();requestAnimationFrame(tick);</script>`);
console.log('SKIN_COMPARISON.html: '+data.length+' paired sequences; '+data.length*72+' PNG references verified');
const review=path.join(out,'REVIEW');fs.mkdirSync(review,{recursive:true});
let md='# M002.A3.1 — comparação Classic / Slim\n\nCapturas reais, mesmos cenários e fases. Aprovação estética pendente. O comparador completo fica em `../SKIN_COMPARISON.html`.\n\n| Cenário / fase QA | Classic | Slim |\n|---|---|---|\n';
for(const [scene,phase]of Object.entries({sword:34,roll:38,bow_shot:50,consume:42,torch:40,crouch_move:40,jump:28})){
 for(const [kind,folder]of [['classic','coverage'],['slim','skincheck-slim']])fs.copyFileSync(path.join(out,folder,'screenshots',scene,String(phase).padStart(3,'0')+'.png'),path.join(review,scene+'-'+kind+'.png'));
 md+='| '+scene+' / '+(phase/20).toFixed(2)+' s | ![Classic]('+scene+'-classic.png) | ![Slim]('+scene+'-slim.png) |\n';
}
md+='\nEste pacote mostra uma seleção de quadros, não mede desempenho nem certifica todos os resource packs, skins ou multiplayer. O JAR final e as verificações estão no relatório técnico do marco.\n';
fs.writeFileSync(path.join(review,'README.md'),md);
