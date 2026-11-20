const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const root=path.resolve(__dirname,'../..'),reports=path.join(root,'animation_pipeline/reports'),logs=path.join(root,'.logs');
const read=p=>JSON.parse(fs.readFileSync(p)),save=(n,x)=>fs.writeFileSync(path.join(reports,n),JSON.stringify(x,null,2));
const result={date:'2026-11-13',profiles:{},limits:['aesthetic approval remains with user','thigh/abdomen contact is not completely solved','CPU draw scope only, not GPU/FPS benchmark','local integrated server, no dedicated multiplayer homologation']};
for(const mode of ['cpm','r2','r3','r31','restart']){
 const r=read(path.join(logs,'r31-'+mode+'.json'));assert(r.checks.length>0&&r.checks.every(x=>x.pass),'Failed profile '+mode);
 save('M002_R3_1_'+mode.toUpperCase()+'_RUNTIME.json',r);result.profiles[mode]={passed:r.checks.filter(x=>x.pass).length,total:r.checks.length,draws:r.draws,meanCpuDrawMs:r.meanCpuDrawMs,maxCpuDrawMs:r.maxCpuDrawMs};
 if(mode==='cpm')result.profiles[mode].cpuMetricScope='Polymesh during later fallback tests; does not measure CPM renderer';
}
const combat=read(path.join(reports,'M002_R3_1_COMBAT_RUNTIME.json'));assert.equal(combat.length,132);assert(combat.every(x=>x.pass),'Combat regression');
const trace=read(path.join(logs,'r31-combat-render.json'));assert(trace.allSyncChecksPassed&&trace.renderErrors===0&&trace.draws>0);save('M002_R3_1_COMBAT_RENDER_TRACE.json',trace);result.combat={passed:132,total:132,impactSamples:trace.impactSamples.length,allSyncChecksPassed:trace.allSyncChecksPassed,draws:trace.draws,meanCpuDrawMs:trace.meanCpuDrawMs,renderErrors:trace.renderErrors};
const native=read(path.join(reports,'M002_R3_1_NATIVE_MORPH_AUDIT.json'));assert.equal(native.length,45);assert(native.every(r=>r.weightSum>11.99&&r.maxSkinnedDeltaBlocks>0&&r.minBaseRingAreaRatio>.98));result.nativeMorph={frames:45,minBaseRingAreaRatio:Math.min(...native.map(r=>r.minBaseRingAreaRatio)),maxSkinnedDeltaBlocks:Math.max(...native.map(r=>r.maxSkinnedDeltaBlocks))};
result.validation=read(path.join(reports,'M002_R3_1_VALIDATION.json'));assert.equal(result.validation.status,'PASS');
const before=read(path.join(reports,'M002_R3_1_DIAGNOSIS_R3.json')),after=read(path.join(reports,'M002_R3_1_DIAGNOSIS_R31.json'));result.crossSections={};for(const name of Object.keys(before.frames[0].meshes))result.crossSections[name]={beforeMinAreaRatio:Math.min(...before.frames.map(r=>r.meshes[name].minRingAreaRatio)),afterMinAreaRatio:Math.min(...after.frames.map(r=>r.meshes[name].minRingAreaRatio))};
result.builds={};for(const name of ['lab-build','production-build']){const bytes=fs.readFileSync(path.join(logs,'r31-'+name+'.log')),text=bytes.toString(bytes[0]===255&&bytes[1]===254?'utf16le':'utf8');assert(text.includes('BUILD SUCCESSFUL'));result.builds[name]=text.match(/BUILD SUCCESSFUL[^\r\n]*/)[0];}
result.normalJar=read(path.join(logs,'r31-jar-audit.json'));assert(result.normalJar.experimentalExcluded&&result.normalJar.privateAssetsExcluded);
result.cpuMeanDeltaMs=result.profiles.r31.meanCpuDrawMs-result.profiles.r3.meanCpuDrawMs;result.cpuMeanDeltaPercent=100*(result.profiles.r31.meanCpuDrawMs/result.profiles.r3.meanCpuDrawMs-1);
const visual=path.join(reports,'M002_R3_1_visual');fs.mkdirSync(visual,{recursive:true});const screenshots=[];
for(const name of fs.readdirSync(path.join(root,'run-r31/screenshots')).filter(n=>/^r31-.*\.png$/.test(n)||/^toe-m002_0-(iron_front|sword-before-impact)\.png$/.test(n))){const b=fs.readFileSync(path.join(root,'run-r31/screenshots',name));fs.writeFileSync(path.join(visual,name),b);screenshots.push({name,sha256:crypto.createHash('sha256').update(b).digest('hex')});}
for(const mode of ['cpm','r2','r3','r31'])for(const pose of ['idle','jog','sword','roll',...[4,11,15,23,26,27,30].map(f=>'roll-f'+f)])for(const yaw of [0,90,180,270])assert(screenshots.some(s=>s.name===`r31-${mode}-${pose}-${yaw}.png`));
result.screenshots=screenshots;const godot=read(path.join(reports,'M001_3_VALIDATION.json'));assert.equal(godot.status,'PASS');result.godotFilesPreserved=godot.godotFilesPreserved;
save('M002_R3_1_TEST_RESULTS.json',result);console.log(JSON.stringify({...result,screenshots:screenshots.length},null,2));
