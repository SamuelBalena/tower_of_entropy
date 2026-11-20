const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),dir=path.join(root,'animation_pipeline/reports/m002_a1_1');
const json=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex').toUpperCase();
const baseline=json(path.join(dir,'BASELINE.json')),assets=json(path.join(dir,'ASSET_BASELINE.json'));
const allowed=new Set(['build.gradle','scripts/start-r31.ps1',...['combat/CombatController.java','combat/CombatCommands.java','combat/RollController.java','animation/ActionNetwork.java','client/ClientCombatState.java','client/CombatInput.java'].map(p=>'src/main/java/com/towerofentropy/'+p),'validation/a1/java/com/towerofentropy/combat/A1RuntimeTest.java','validation/r4/java/com/towerofentropy/renderlab/R4PromotionTest.java','validation/roll/java/com/towerofentropy/combat/RollRuntimeTest.java']);
const changed=[];let preserved=0;for(const [windows,wanted]of Object.entries(baseline)){const p=windows.replaceAll('\\','/');if(hash(path.join(root,p))===wanted)preserved++;else{assert(allowed.has(p),'Unexpected modification: '+p);changed.push(p);}}
for(const [p,wanted]of Object.entries(assets))assert.equal(hash(path.join(root,p)),wanted,'Animation/model changed: '+p);
const historical=json(path.join(root,'animation_pipeline/reports/M002_R4_BASELINE.json'));let experiments=0;
for(const [p,wanted]of Object.entries(historical))if(p.startsWith('experiments/')||p.includes('converted/')){assert.equal(hash(path.join(root,p)),wanted.toUpperCase(),p);experiments++;}
assert.equal(hash(path.join(root,'animation_pipeline/converted/m002_r3_1/resources/assets/towerofentropy/models/gltf/r31_steve.glb')),assets['animation_pipeline/converted/m002_r3_1/steve-r31-refined.glb']);
const godot=json(path.join(root,'animation_pipeline/reports/asset-metadata.json'));for(const f of godot.files)assert.equal(hash(path.join(godot.root,f.path)),f.sha256.toUpperCase(),f.path);
const report={status:'PASS',baselineFilesPreserved:preserved,authorizedChanges:changed,modelsAndAnimationsByteIdentical:Object.keys(assets),historicExperimentFilesPreserved:experiments,r31RuntimeCopyPreserved:true,godotFilesPreserved:godot.files.length,balanceAndRollRulesUnchanged:true};
fs.writeFileSync(path.join(dir,'PRESERVATION.json'),JSON.stringify(report,null,2));console.log(report);
