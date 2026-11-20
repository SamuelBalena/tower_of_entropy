const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const historic=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R4_BASELINE.json')));
let approved=0;for(const [p,h]of Object.entries(historic))if(p.startsWith('experiments/')||p.includes('converted/')){assert.equal(hash(path.join(root,p)),h,p);approved++;}
const baseline=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/m002_a1/BASELINE.json'),'utf8').replace(/^\uFEFF/,''));
const allowed=new Set(['build.gradle','animation_pipeline/scripts/prepare-r4.cjs',...['animation/ActionNetwork.java','animation/LocomotionController.java','combat/CombatConfig.java','combat/CombatController.java','combat/RollController.java','combat/CombatCommands.java','client/PlayerRendererR3.java'].map(p=>'src/main/java/com/towerofentropy/'+p)]);
const changed=[];let unchanged=0;for(const [windows,h]of Object.entries(baseline)){const p=windows.replaceAll('\\','/');if(hash(path.join(root,p)).toUpperCase()===h)unchanged++;else{assert(allowed.has(p),'Unexpected modification '+p);changed.push(p);}}
function glb(p){const b=fs.readFileSync(path.join(root,p));let g,bin;for(let o=12;o<b.length;){const n=b.readUInt32LE(o),t=b.readUInt32LE(o+4);o+=8;if(t===0x4e4f534a)g=JSON.parse(b.subarray(o,o+n));else if(t===0x004e4942)bin=b.subarray(o,o+n);o+=n;}return {g,bin};}
const a=glb('animation_pipeline/converted/m002_r3/steve-r3-refined.glb'),b=glb('animation_pipeline/converted/m002_r4/resources/assets/towerofentropy/models/gltf/steve_r3.glb');
for(const key of ['nodes','meshes','skins','materials','textures','images'])assert.deepEqual(a.g[key],b.g[key],key);
assert.deepEqual(a.bin,b.bin.subarray(0,a.bin.length),'Original geometry/UV/weight/animation bytes changed');
assert.deepEqual(a.g.animations,b.g.animations.slice(0,a.g.animations.length));
const godot=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/asset-metadata.json')));for(const f of godot.files)assert.equal(hash(path.join(godot.root,f.path)),f.sha256,f.path);
const report={status:'PASS',approvedModelsAndExperimentalFilesPreserved:approved,baselineFilesUnchanged:unchanged,authorizedChanges:changed,godotFilesPreserved:godot.files.length,originalR3GeometryRigUVWeightsAndFourAnimationsUnchanged:true,runtimeAnimationNames:b.g.animations.map(a=>a.name),originalTargetingUnchanged:!changed.some(p=>p.includes('SwordTargeting'))};
fs.writeFileSync(path.join(root,'animation_pipeline/reports/m002_a1/PRESERVATION.json'),JSON.stringify(report,null,2));console.log(report);
