const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const base=path.resolve(__dirname,'..'),root=path.resolve(base,'..'),hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const baseline=JSON.parse(fs.readFileSync(path.join(base,'reports/M001_3_BASELINE.json')));
for(const[p,sha]of Object.entries(baseline))assert.equal(hash(path.join(root,p)),sha,'Approved model changed: '+p);
const data=JSON.parse(fs.readFileSync(path.join(base,'converted/m001_3/actions-data.json')));
const ids=new Set([0,1,2,3,4,5]);let cubes=0,anchors=0;
function walk(e){for(const c of e.children||[]){assert(!ids.has(c.storeID));ids.add(c.storeID);if(c.itemRenderer){anchors++;assert.deepEqual(c.size,{x:0,y:0,z:0});}else cubes++;walk(c);}}
data['config.json'].elements.forEach(walk);assert.equal(cubes,16);assert.equal(anchors,2);
for(const[name,duration,frames]of[['toe_roll',1467,45],['toe_sword',433,14]]){
 const a=data['animations/g_'+name+'.json'];assert.equal(a.duration,duration);assert.equal(a.frames.length,frames);
 assert(!a.loop&&!a.mustFinish&&a.command&&!a.layerControlled);assert.equal(a.priority,10);assert.equal(a.interpolator,'linear_single');
 for(const f of a.frames){assert.equal(f.components.length,12);for(const c of f.components){assert(ids.has(c.storeID));for(const v of [...Object.values(c.rotation),...Object.values(c.pos)])assert(Number.isFinite(v));for(const v of Object.values(c.rotation))assert(v>=0&&v<360);assert.deepEqual(c.scale,{x:1,y:1,z:1});}}
}
for(const[name,duration]of[['toe_idle',2500],['toe_jog',933]]){const a=data['animations/c_'+name+'.json'];assert(a.loop);assert.equal(a.duration,duration);assert.equal(a.priority,0);}
const inv=JSON.parse(fs.readFileSync(path.join(base,'reports/asset-metadata.json')));
for(const f of inv.files)assert.equal(hash(path.join(inv.root,f.path)),f.sha256,'Godot source changed');
const result={status:'PASS',approvedFilesPreserved:Object.keys(baseline).length,godotFilesPreserved:inv.files.length,cubes,anchors,oneShots:{Roll:{durationMs:1467,frames:45},Sword_Regular_A:{durationMs:433,frames:14}},bodyRig:'unchanged Classic with invisible wrist/item anchors',loop:false,priority:10};
fs.writeFileSync(path.join(base,'reports/M001_3_VALIDATION.json'),JSON.stringify(result,null,2));console.log(result);
