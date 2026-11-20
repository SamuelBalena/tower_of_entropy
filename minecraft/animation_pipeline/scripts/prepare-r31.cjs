const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert/strict'),{zip}=require('./zip-project.cjs');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'animation_pipeline/converted/m002_r3_1');fs.mkdirSync(out,{recursive:true});
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex'),bp=path.join(root,'animation_pipeline/reports/M002_R3_1_BASELINE.json');
if(!fs.existsSync(bp)){
 const b=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R3_BASELINE.json')));
 function add(p){for(const e of fs.readdirSync(path.join(root,p),{withFileTypes:true})){const f=p+'/'+e.name;if(e.isDirectory())add(f);else b[f]=hash(f);}}
 add('experiments/m002_r3');for(const p of ['animation_pipeline/scripts/prepare-r3.cjs','animation_pipeline/scripts/blender-r3-refine.py','animation_pipeline/scripts/validate-r3.cjs','animation_pipeline/scripts/collect-r3.cjs','scripts/start-r3.ps1','docs/M002_R3_CHARACTER_REFINEMENT.md','docs/TEST_LOG_M002_R3.md','animation_pipeline/converted/m002_r3/steve-r3-refined.blend','animation_pipeline/converted/m002_r3/steve-r3-refined.glb'])b[p]=hash(p);
 fs.writeFileSync(bp,JSON.stringify(b,null,2));for(const p of Object.keys(b)){const d=path.join(root,'.downloads/m002_r3_1/backup',p);fs.mkdirSync(path.dirname(d),{recursive:true});fs.copyFileSync(path.join(root,p),d);}
}
const baseline=JSON.parse(fs.readFileSync(bp));for(const[p,h]of Object.entries(baseline))assert.equal(hash(p),h,p);
require('child_process').execFileSync(process.execPath,[path.join(__dirname,'validate-r31.cjs'),'--sample-only'],{stdio:'inherit'});
const assets=path.join(out,'resources/assets/towerofentropy');fs.mkdirSync(path.join(assets,'models/gltf'),{recursive:true});
for(const[n,p]of [['r31_reference','m002_r1/steve-r1-weighted.glb'],['r31_r3','m002_r3/steve-r3-refined.glb']])fs.copyFileSync(path.join(root,'animation_pipeline/converted',p),path.join(assets,'models/gltf',n+'.glb'));
fs.copyFileSync(path.join(root,'animation_pipeline/converted/m002_r1/poses.json'),path.join(assets,'r31-poses.json'));
if(fs.existsSync(path.join(out,'steve-r31-refined.glb')))fs.copyFileSync(path.join(out,'steve-r31-refined.glb'),path.join(assets,'models/gltf/r31_steve.glb'));
// Freeze exact original CPM frames for equivalent four-way comparisons.
const original=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/converted/m001_3/actions-data.json'))),a=original['animations/g_toe_roll.json'];
for(const frame of [4,11,15,23,26,27,30]){const anim={...a,name:'toe_r1',loop:true,command:true,layerControlled:false,duration:1000,frames:[structuredClone(a.frames[frame-1])],interpolator:'linear_loop',priority:20,mustFinish:false};fs.writeFileSync(path.join(out,`baseline-roll-${frame}.cpmproject`),zip({'config.json':original['config.json'],'animations/c_toe_r1.json':anim}));}
console.log('R3.1 baseline preserved:',Object.keys(baseline).length);
