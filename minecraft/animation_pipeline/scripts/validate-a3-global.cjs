const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'animation_pipeline/reports/m002_a3_1');
const hash=b=>crypto.createHash('sha256').update(b).digest('hex').toUpperCase();
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8').replace(/^\uFEFF/,''));
function glb(p){const b=fs.readFileSync(path.join(root,p)),n=b.readUInt32LE(12);return {g:JSON.parse(b.subarray(20,20+n)),bin:b.subarray(28+n),b};}
const c=glb('animation_pipeline/converted/m002_a3/resources/assets/towerofentropy/models/gltf/steve_a3.glb'),s=glb('animation_pipeline/converted/m002_a3_1/resources/assets/towerofentropy/models/gltf/steve_a3_slim.glb');
assert.equal(hash(c.b),read('animation_pipeline/reports/m002_a3_1/CLASSIC_BASELINE.json').Hash);
for(const k of ['nodes','skins','animations','materials','images','textures','bufferViews'])assert.deepEqual(c.g[k],s.g[k],k);
const mutable=new Set(),arms=[];
const value=(model,id,i,k)=>{const a=model.g.accessors[id],v=model.g.bufferViews[a.bufferView],w={VEC2:2,VEC3:3}[a.type];return model.bin.readFloatLE((v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||w*4)+k*4);};
for(const m of c.g.meshes){const arm=/^limb_[23](_outer)?$/.test(m.name);for(const p of m.primitives){for(const [key,id]of Object.entries(p.attributes)){
 const a=c.g.accessors[id],v=c.g.bufferViews[a.bufferView];if(arm&&['POSITION','TEXCOORD_0'].includes(key)){for(let i=v.byteOffset||0;i<(v.byteOffset||0)+v.byteLength;i++)mutable.add(i);}else assert(c.bin.subarray(v.byteOffset||0,(v.byteOffset||0)+v.byteLength).equals(s.bin.subarray(v.byteOffset||0,(v.byteOffset||0)+v.byteLength)),m.name+' '+key);
 }if(arm){const a=s.g.accessors[p.attributes.POSITION],width=(a.max[0]-a.min[0])*16;assert(Math.abs(width-(m.name.endsWith('outer')?3.5:3))<1e-5);
 const faces={},u=m.name.startsWith('limb_2')?(m.name.endsWith('outer')?48:32):40;
 const expected={'-1,0,0':[0,4],'0,0,-1':[4,7],'1,0,0':[7,11],'0,0,1':[11,14],'0,1,0':[4,7],'0,-1,0':[7,10]};
 for(let i=0;i<a.count;i++){
  for(const k of [1,2])assert.equal(value(s,p.attributes.POSITION,i,k),value(c,p.attributes.POSITION,i,k),'arm height/depth');
  assert.equal(value(s,p.attributes.TEXCOORD_0,i,1),value(c,p.attributes.TEXCOORD_0,i,1),'UV vertical coordinate');
  const n=[0,1,2].map(k=>Math.round(value(s,p.attributes.NORMAL,i,k))).join(',');(faces[n]??=[]).push(value(s,p.attributes.TEXCOORD_0,i,0)*64-u);
 }
 for(const [face,range]of Object.entries(expected)){assert(faces[face]);assert(Math.abs(Math.min(...faces[face])-range[0])<1e-5,face+' min');assert(Math.abs(Math.max(...faces[face])-range[1])<1e-5,face+' max');}
 arms.push({mesh:m.name,widthPixels:width,sixOfficialUvFaceBoundsVerified:true});}}}
let changed=0;assert.equal(c.bin.length,s.bin.length);for(let i=0;i<c.bin.length;i++)if(c.bin[i]!==s.bin[i]){assert(mutable.has(i),'unexpected binary change '+i);changed++;}
for(const [p,h]of Object.entries(read('animation_pipeline/reports/m002_a1_2/ASSET_BASELINE.json')))assert.equal(hash(fs.readFileSync(path.join(root,p))),h,p);
const godot=read('animation_pipeline/reports/asset-metadata.json');for(const f of godot.files)assert.equal(hash(fs.readFileSync(path.join(godot.root,f.path))).toLowerCase(),f.sha256,f.path);
const combat=path.join(root,'src/main/java/com/towerofentropy/combat'),backup=path.join(root,'.downloads/m002_a3_1/baseline/src/main/java/com/towerofentropy/combat');for(const f of fs.readdirSync(combat))assert(fs.readFileSync(path.join(combat,f)).equals(fs.readFileSync(path.join(backup,f))),f);
const allowed=new Set(['main/java/com/towerofentropy/animation/ActionKeys.java','main/java/com/towerofentropy/animation/ActionNetwork.java','main/java/com/towerofentropy/animation/CompatibilityModels.java','main/java/com/towerofentropy/animation/LocomotionController.java','main/java/com/towerofentropy/client/ClientCombatState.java','main/java/com/towerofentropy/client/PlayerRendererR3.java']),sourceChanges=[];
function walk(dir,prefix=''){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name),prefix+e.name+'/'):[prefix+e.name]);}
const baseSrc=path.join(root,'.downloads/m002_a3_1/baseline/src'),currentSrc=path.join(root,'src');const beforeFiles=walk(baseSrc),afterFiles=walk(currentSrc);assert.deepEqual(afterFiles.sort(),beforeFiles.sort(),'No added/removed main-source files');for(const f of beforeFiles)if(hash(fs.readFileSync(path.join(baseSrc,f)))!==hash(fs.readFileSync(path.join(currentSrc,f)))){assert(allowed.has(f),'unexpected main source change '+f);sourceChanges.push(f);}
const result={status:'PASS',classicSha256:hash(c.b),slimSha256:hash(s.b),animations:c.g.animations.length,arms,changedArmGeometryUvBytes:changed,allOtherBinaryBytesIdentical:true,combatServerFilesUnchanged:true,godotFilesPreserved:godot.files.length,mainSourceChanges:sourceChanges};fs.writeFileSync(path.join(out,'PRESERVATION.json'),JSON.stringify(result,null,2));console.log(result);
