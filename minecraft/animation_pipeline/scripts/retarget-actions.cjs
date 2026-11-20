// M001.3: reuse original GLB retargeting; leave approved M001.1/M001.2 files untouched.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process'),{zip}=require('./zip-project.cjs');
const base=path.resolve(__dirname,'..'),out=path.join(base,'converted/m001_3');
for(const option of ['--roll','--sword']){const r=spawnSync(process.execPath,[path.join(__dirname,'retarget-idle.cjs'),option],{stdio:'inherit'});if(r.status!==0)process.exit(r.status||1);}
const locomotion=JSON.parse(fs.readFileSync(path.join(base,'converted/m001_2/locomotion-data.json')));
const roll=JSON.parse(fs.readFileSync(path.join(out,'roll-data.json'))),sword=JSON.parse(fs.readFileSync(path.join(out,'sword-data.json')));
assert.deepEqual(roll.config,sword.config,'UAL1 and UAL2 must map to identical target rig');
// Added zero-size item anchors do not replace any approved visible cube or skin UV.
function visible(config){const c=structuredClone(config);function strip(e){e.children=(e.children||[]).filter(c=>!c.itemRenderer);e.children.forEach(strip);}c.elements.forEach(strip);return c;}
assert.deepEqual(visible(roll.config),visible(locomotion['config.json']));
const entries={'config.json':roll.config};
for(const key of ['animations/c_toe_idle.json','animations/c_toe_jog.json']){
 const a=structuredClone(locomotion[key]);
 for(const f of a.frames)for(const id of[302,303])f.components.push({storeID:id,pos:{x:0,y:-4,z:0},rotation:{x:0,y:0,z:0},scale:{x:1,y:1,z:1},color:'ffffff',show:true});
 entries[key]=a;
}
for(const [name,data]of [['toe_roll',roll],['toe_sword',sword]])entries['animations/g_'+name+'.json']={...data.animation,name,loop:false,command:true,hidden:true,layerControlled:false,mustFinish:false,priority:10};
fs.writeFileSync(path.join(out,'steve-ual-actions.cpmproject'),zip(entries));
fs.writeFileSync(path.join(out,'actions-data.json'),JSON.stringify(entries,null,2));
console.log('Action project: Idle/Jog + one-shot Roll (1467ms,45 frames) / Sword (433ms,14 frames), priority 10.');
