// Build mutually exclusive CPM custom poses from the unchanged M001.1 rig/Idle and UAL1 Jog.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process'),{zip}=require('./zip-project.cjs');
const base=path.resolve(__dirname,'..');
for(const args of [[],['--jog']]){const r=spawnSync(process.execPath,[path.join(__dirname,'retarget-idle.cjs'),...args],{stdio:'inherit'});if(r.status!==0)process.exit(r.status||1);}
const idle=JSON.parse(fs.readFileSync(path.join(base,'converted/m001_1/retarget-data.json')));
const jog=JSON.parse(fs.readFileSync(path.join(base,'converted/m001_2/jog-data.json')));
assert.deepEqual(idle.config,jog.config,'Jog must use the same articulated Steve');
// Use CPM's command/network parameter channel, not vanilla skin-layer bits.
const animation=(a,name)=>({...a,name,command:true,hidden:true,layerControlled:false,mustFinish:false});
const entries={'config.json':idle.config,'animations/c_toe_idle.json':animation(idle.animation,'toe_idle'),'animations/c_toe_jog.json':animation(jog.animation,'toe_jog')};
const out=path.join(base,'converted/m001_2');
fs.writeFileSync(path.join(out,'steve-ual1-locomotion.cpmproject'),zip(entries));
fs.writeFileSync(path.join(out,'locomotion-data.json'),JSON.stringify(entries,null,2));
console.log('Locomotion project: exclusive custom poses toe_idle (2500ms), toe_jog (933ms).');
