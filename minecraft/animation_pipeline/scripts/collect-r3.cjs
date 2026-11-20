// Collect completed runs only. A green Gradle exit alone is never a visual/combat pass.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const root=path.resolve(__dirname,'../..'),reports=path.join(root,'animation_pipeline/reports'),logs=path.join(root,'.logs');
const read=p=>JSON.parse(fs.readFileSync(p)),save=(n,x)=>fs.writeFileSync(path.join(reports,n),JSON.stringify(x,null,2));
const result={date:'2026-11-12',profiles:{},limits:['CPU draw scope only; not GPU/FPS benchmark','local integrated server and client replica; no dedicated multiplayer homologation','cape test validates fallback decision using local texture fixture','aesthetic approval remains with user']};
for(const mode of ['cpm','r2','r3','stress','restart']){
 const r=read(path.join(logs,'r3-'+mode+'.json'));assert(r.checks.length>0);assert(r.checks.every(x=>x.pass),'Failed profile '+mode);
 save('M002_R3_'+mode.toUpperCase()+'_RUNTIME.json',r);
 result.profiles[mode]={passed:r.checks.filter(x=>x.pass).length,total:r.checks.length,draws:r.draws,meanCpuDrawMs:r.meanCpuDrawMs,maxCpuDrawMs:r.maxCpuDrawMs};
 if(mode==='cpm')result.profiles[mode].cpuMetricScope='Polymesh calls during later fallback tests only; does not measure CPM renderer cost';
}
save('M002_R3_INITIAL_RUNTIME.json',read(path.join(logs,'r3-r3-initial.json')));
const combat=read(path.join(reports,'M002_R3_COMBAT_RUNTIME.json'));assert.equal(combat.length,132);assert(combat.every(x=>x.pass),'Combat regression');
const trace=read(path.join(logs,'r3-combat-render.json'));assert(trace.allSyncChecksPassed&&trace.renderErrors===0&&trace.draws>0,'Render/combat sync');save('M002_R3_COMBAT_RENDER_TRACE.json',trace);
result.combat={passed:combat.filter(x=>x.pass).length,total:combat.length,impactSamples:trace.impactSamples.length,allSyncChecksPassed:trace.allSyncChecksPassed,renderErrors:trace.renderErrors};
result.cpuMeanDeltaMs=result.profiles.r3.meanCpuDrawMs-result.profiles.r2.meanCpuDrawMs;
result.cpuMeanDeltaPercent=100*(result.profiles.r3.meanCpuDrawMs/result.profiles.r2.meanCpuDrawMs-1);
result.builds={};for(const name of ['lab-build','production-build']){const data=fs.readFileSync(path.join(logs,'r3-'+name+'.log')),text=data.toString(data[0]===255&&data[1]===254?'utf16le':'utf8');assert(text.includes('BUILD SUCCESSFUL'),name);result.builds[name]=text.match(/BUILD SUCCESSFUL[^\r\n]*/)[0];}
const jar=read(path.join(logs,'r3-jar-audit.json'));assert(jar.experimentalExcluded&&jar.privateAssetsExcluded);result.normalJar=jar;
const visual=path.join(reports,'M002_R3_visual');fs.mkdirSync(visual,{recursive:true});const manifest=[];
for(const name of fs.readdirSync(path.join(root,'run-r3/screenshots')).filter(n=>/^r3-.*\.png$/.test(n)||/^toe-m002_0-(iron_front|sword-before-impact)\.png$/.test(n))){
 const data=fs.readFileSync(path.join(root,'run-r3/screenshots',name));fs.writeFileSync(path.join(visual,name),data);manifest.push({name,sha256:crypto.createHash('sha256').update(data).digest('hex')});
}
for(const mode of ['cpm','r2','r3'])for(const clip of ['idle','jog','sword','roll'])for(const yaw of [0,90,180,270])assert(manifest.some(x=>x.name===`r3-${mode}-${clip}-${yaw}.png`));
for(const pose of ['jog-90','sword-270','roll-90','roll-270'])assert(manifest.some(x=>x.name===`r3-stress-${pose}.png`));
result.screenshots=manifest;result.validation=read(path.join(reports,'M002_R3_VALIDATION.json'));assert.equal(result.validation.status,'PASS');
const previous=read(path.join(reports,'M001_3_VALIDATION.json'));assert.equal(previous.status,'PASS');result.godotFilesPreserved=previous.godotFilesPreserved;
save('M002_R3_TEST_RESULTS.json',result);console.log(JSON.stringify({...result,screenshots:manifest.length},null,2));
