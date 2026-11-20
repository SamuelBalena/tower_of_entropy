// Read-only inspection and isolated copy of the R1 GLB; no model reconstruction.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),src=path.join(root,'animation_pipeline/converted/m002_r1/steve-r1-weighted.glb'),out=path.join(root,'animation_pipeline/converted/m002_r2');
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
fs.mkdirSync(out,{recursive:true});
const baselinePath=path.join(root,'animation_pipeline/reports/M002_R2_BASELINE.json');
if(!fs.existsSync(baselinePath)){
 const previous=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R1_BASELINE.json')));
 for(const f of ['animation_pipeline/converted/m002_r1/steve-r1-weighted.glb','animation_pipeline/converted/m002_r1/steve-r1-weighted.blend','animation_pipeline/scripts/blender-r1-weighted.py'])previous[f]=hash(path.join(root,f));
 fs.writeFileSync(baselinePath,JSON.stringify(previous,null,2));
 for(const p of Object.keys(previous)){const dest=path.join(root,'.downloads/m002_r2/backup',p);fs.mkdirSync(path.dirname(dest),{recursive:true});fs.copyFileSync(path.join(root,p),dest);}
}
const baseline=JSON.parse(fs.readFileSync(baselinePath));for(const[p,h]of Object.entries(baseline))assert.equal(hash(path.join(root,p)),h,p);
const resources=path.join(out,'resources/assets/towerofentropy');fs.mkdirSync(path.join(resources,'models/gltf'),{recursive:true});
fs.copyFileSync(src,path.join(resources,'models/gltf/r2_steve.glb'));
fs.copyFileSync(path.join(root,'animation_pipeline/converted/m002_r1/poses.json'),path.join(resources,'r2-poses.json'));
const b=fs.readFileSync(src),j=JSON.parse(b.subarray(20,20+b.readUInt32LE(12)));
fs.writeFileSync(path.join(root,'animation_pipeline/reports/M002_R2_GLTF_INSPECTION.json'),JSON.stringify({sha256:hash(src),nodes:j.nodes.map((n,i)=>({index:i,name:n.name,children:n.children,skin:n.skin,mesh:n.mesh})),skins:j.skins,materials:j.materials,animations:j.animations.map(a=>({name:a.name,channels:a.channels.length})),unchangedFiles:Object.keys(baseline).length},null,2));
console.log('R2 prepared: original GLB byte-identical; preserved files:',Object.keys(baseline).length);
