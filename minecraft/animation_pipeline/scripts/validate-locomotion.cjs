const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const base=path.resolve(__dirname,'..'),root=path.resolve(base,'..'),hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const baseline=JSON.parse(fs.readFileSync(path.join(base,'reports/M001_2_BASELINE.json')));
for(const [p,sha]of Object.entries(baseline))assert.equal(hash(path.join(root,p)),sha,'M001.1 changed: '+p);
const all=JSON.parse(fs.readFileSync(path.join(base,'converted/m001_2/locomotion-data.json')));
const idle=all['animations/c_toe_idle.json'],jog=all['animations/c_toe_jog.json'];
assert.equal(idle.duration,2500);assert.equal(idle.frames.length,75);assert.equal(jog.duration,933);assert.equal(jog.frames.length,28);
assert.deepEqual(jog.frames[0].components.map(c=>c.storeID),idle.frames[0].components.map(c=>c.storeID));
for(const a of[idle,jog]){assert(a.loop&&a.command&&!a.additive);for(const f of a.frames){assert.equal(f.components.length,10);for(const c of f.components){for(const n of [...Object.values(c.pos),...Object.values(c.rotation)])assert(Number.isFinite(n));assert.deepEqual(c.scale,{x:1,y:1,z:1});for(const n of Object.values(c.rotation))assert(n>=0&&n<360);}}}
const source=JSON.parse(fs.readFileSync(path.join(base,'reports/M001_2_SOURCE_ANALYSIS.json')));
assert.equal(source.clip,'Jog_Fwd_Loop');assert(Math.abs(source.duration-28/30)<1e-6);assert.equal(source.frames,28);
const inventory=JSON.parse(fs.readFileSync(path.join(base,'reports/asset-metadata.json')));
for(const f of inventory.files)assert.equal(hash(path.join(inventory.root,f.path)),f.sha256,'Godot source changed');
const result={status:'PASS',m001_1BaselinePreserved:true,godotFilesPreserved:inventory.files.length,source:'UAL1 Jog_Fwd_Loop',durationMs:933,frames:28,sameRig:true,exclusiveCustomPoses:['toe_idle','toe_jog'],rootMotionPolicy:'Source translations discarded; controller observes movement only'};
fs.writeFileSync(path.join(base,'reports/M001_2_VALIDATION.json'),JSON.stringify(result,null,2));console.log(result);
