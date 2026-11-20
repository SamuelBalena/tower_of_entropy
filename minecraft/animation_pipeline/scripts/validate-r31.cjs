const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const root=path.resolve(__dirname,'../..'),hash=p=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex');
const baseline=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R3_1_BASELINE.json')));for(const[p,h]of Object.entries(baseline))assert.equal(hash(p),h,p);
function load(rel){const b=fs.readFileSync(path.join(root,rel));let g,bin;for(let o=12;o<b.length;){const n=b.readUInt32LE(o),t=b.readUInt32LE(o+4);o+=8;if(t===0x4e4f534a)g=JSON.parse(b.subarray(o,o+n));else if(t===0x004e4942)bin=b.subarray(o,o+n);o+=n;}
 function acc(id){const a=g.accessors[id],v=g.bufferViews[a.bufferView],w={SCALAR:1,VEC2:2,VEC3:3,VEC4:4,MAT4:16}[a.type],bytes={5121:1,5123:2,5125:4,5126:4}[a.componentType];assert(w&&bytes);return Array.from({length:a.count},(_,i)=>Array.from({length:w},(_,k)=>{const o=(v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||w*bytes)+k*bytes;let x=a.componentType===5126?bin.readFloatLE(o):a.componentType===5121?bin.readUInt8(o):a.componentType===5123?bin.readUInt16LE(o):bin.readUInt32LE(o);if(a.normalized)x/=a.componentType===5121?255:65535;assert(Number.isFinite(x));return x;}));}
 const parents={};g.nodes.forEach((n,i)=>(n.children||[]).forEach(j=>parents[j]=i));const animation=Object.fromEntries(g.animations.map(a=>[a.name,a.channels.map(c=>{const s=a.samplers[c.sampler];return {node:c.target.node,path:c.target.path,t:acc(s.input).flat(),v:acc(s.output),mode:s.interpolation||'LINEAR'};})]));
 function pose(name,t){const l=g.nodes.map(n=>({p:[...(n.translation||[0,0,0])],q:[...(n.rotation||[0,0,0,1])],s:[...(n.scale||[1,1,1])]}));for(const c of animation[name]||[]){let i=0;while(i+1<c.t.length&&c.t[i+1]<=t+1e-7)i++;const f=i===c.t.length-1||c.mode==='STEP'?0:Math.max(0,(t-c.t[i])/(c.t[i+1]-c.t[i]));const value=f===0?c.v[i]:c.path==='rotation'?slerp(c.v[i],c.v[i+1],f):c.v[i].map((v,k)=>v+(c.v[i+1][k]-v)*f);l[c.node][{translation:'p',rotation:'q',scale:'s'}[c.path]]=value;}const world=[];function get(i){if(world[i])return world[i];const m=g.nodes[i].matrix||compose(l[i]);return world[i]=parents[i]===undefined?m:mul(get(parents[i]),m);}g.nodes.forEach((_,i)=>get(i));return world;}
 return {g,acc,pose,index:n=>g.nodes.findIndex(x=>x.name===n)};
}
function slerp(a,b,t){let d=a.reduce((s,v,i)=>s+v*b[i],0);if(d<0){b=b.map(v=>-v);d=-d;}let q;if(d>.9995)q=a.map((v,i)=>v+(b[i]-v)*t);else{const h=Math.acos(Math.min(1,d));q=a.map((v,i)=>(v*Math.sin((1-t)*h)+b[i]*Math.sin(t*h))/Math.sin(h));}const n=Math.hypot(...q);return q.map(v=>v/n);}
function compose({p,q,s}){const[x,y,z,w]=q;return [(1-2*y*y-2*z*z)*s[0],(2*x*y+2*w*z)*s[0],(2*x*z-2*w*y)*s[0],0,(2*x*y-2*w*z)*s[1],(1-2*x*x-2*z*z)*s[1],(2*y*z+2*w*x)*s[1],0,(2*x*z+2*w*y)*s[2],(2*y*z-2*w*x)*s[2],(1-2*x*x-2*y*y)*s[2],0,...p,1];}
function mul(a,b){return Array.from({length:16},(_,i)=>{const row=i%4,col=Math.floor(i/4);let v=0;for(let k=0;k<4;k++)v+=a[k*4+row]*b[col*4+k];return v;});}
if(process.argv.includes('--sample-only')){
 const source=load('animation_pipeline/converted/m002_r3/steve-r3-refined.glb'),names=['pelvis','waist','0','1','2','3','4','5','102','103','104','105'],times=require('./r31-corrective-times.json'),folder=path.join(root,'animation_pipeline/converted/m002_r3_1');
 fs.mkdirSync(folder,{recursive:true});fs.writeFileSync(path.join(folder,'gltf-roll-samples.json'),JSON.stringify(times.map(time=>{const m=source.pose('UAL_roll',time);return {time,bones:Object.fromEntries(names.map(n=>[n,m[source.index(n)]]))};})));
 const bind=source.pose('',0);fs.writeFileSync(path.join(folder,'gltf-roll-bind.json'),JSON.stringify(Object.fromEntries(names.map(n=>[n,bind[source.index(n)]]))));console.log('R3 glTF source transforms sampled:',times.length);process.exit(0);
}
const old=load('animation_pipeline/converted/m002_r3/steve-r3-refined.glb'),next=load('animation_pipeline/converted/m002_r3_1/steve-r31-refined.glb');assert.equal(next.g.animations.length,4);assert.equal(next.g.skins[0].joints.length,12);
let weighted=0,blended=0,torsoBlended=0,maxWeightError=0;
for(const n of next.g.nodes)if(n.mesh!==undefined)for(const primitive of next.g.meshes[n.mesh].primitives){for(const w of next.acc(primitive.attributes.WEIGHTS_0)){weighted++;const error=Math.abs(w.reduce((a,b)=>a+b,0)-1);maxWeightError=Math.max(maxWeightError,error);assert(error<1e-5);if(w.filter(x=>x>0).length>1){blended++;if(n.name.startsWith('body'))torsoBlended++;}}for(const uv of next.acc(primitive.attributes.TEXCOORD_0))assert(uv.every(x=>x>=0&&x<=1));}
assert(torsoBlended>0);
// Verify every mesh keeps its rest extents and all untouched UVs remain identical.
let maxRestBoundsError=0;
for(const n of next.g.nodes)if(n.mesh!==undefined){
 const o=old.g.nodes.find(x=>x.name===n.name),p=next.g.meshes[n.mesh].primitives[0],op=old.g.meshes[o.mesh].primitives[0];
 const positions=next.acc(p.attributes.POSITION),before=old.acc(op.attributes.POSITION);
 for(let axis=0;axis<3;axis++)for(const fn of [Math.min,Math.max])maxRestBoundsError=Math.max(maxRestBoundsError,Math.abs(fn(...positions.map(v=>v[axis]))-fn(...before.map(v=>v[axis]))));
 const uvs=next.acc(p.attributes.TEXCOORD_0),previousUvs=old.acc(op.attributes.TEXCOORD_0);
 const keys=uvs.map(v=>v.map(x=>x.toFixed(6)).join(',')).sort(),oldKeys=previousUvs.map(v=>v.map(x=>x.toFixed(6)).join(',')).sort();
 if(!n.name.startsWith('body')&&!n.name.startsWith('limb_4')&&!n.name.startsWith('limb_5')){
  const pairs=(positions,uvs)=>[...new Set(positions.map((p,i)=>p.map(x=>x.toFixed(5)).join(',')+'|'+uvs[i].map(x=>x.toFixed(6)).join(',')))].sort();
  assert.deepEqual(pairs(positions,uvs),pairs(before,previousUvs),'Position/UV pairs unchanged; export normal splits ignored: '+n.name);
 }
 else for(const k of new Set(oldKeys))assert(keys.includes(k),'Original torso UV corner missing');
 for(const normal of next.acc(p.attributes.NORMAL))assert(Math.abs(Math.hypot(...normal)-1)<1e-5);
 for(const indices of next.acc(p.attributes.JOINTS_0))assert(indices.every(i=>i>=0&&i<12));
}
assert(maxRestBoundsError<1e-6,'Classic proportions changed');
let maxBindError=0;const bind=next.pose('',0),skin=next.g.skins[0],ibm=next.acc(skin.inverseBindMatrices);for(let i=0;i<skin.joints.length;i++){const m=mul(bind[skin.joints[i]],ibm[i]);for(let k=0;k<16;k++)maxBindError=Math.max(maxBindError,Math.abs(m[k]-(k%5===0?1:0)));}assert(maxBindError<1e-5);
let maxOldOrientationError=0,maxOriginalBonePositionError=0;const durations={};
const oldNames=['pelvis','waist','0','1','2','3','4','5','102','103','104','105'];
for(const a of next.g.animations){const times=next.acc(a.samplers[0].input).flat(),end=times.at(-1);const previous=old.g.animations.find(x=>x.name===a.name);const oldEnd=old.acc(previous.samplers[0].input).flat().at(-1);assert(Math.abs(end-oldEnd)<1e-6);durations[a.name]=end;
 for(const t of times){const om=old.pose(a.name,t),nm=next.pose(a.name,t);for(const name of oldNames){const m=om[old.index(name)],n=nm[next.index(name)];for(let k=12;k<15;k++)maxOriginalBonePositionError=Math.max(maxOriginalBonePositionError,Math.abs(m[k]-n[k]));for(let col=0;col<3;col++){const lm=Math.hypot(m[col*4],m[col*4+1],m[col*4+2]),ln=Math.hypot(n[col*4],n[col*4+1],n[col*4+2]);for(let row=0;row<3;row++)maxOldOrientationError=Math.max(maxOldOrientationError,Math.abs(m[col*4+row]/lm-n[col*4+row]/ln));}}}
}
assert(maxOldOrientationError<2e-4,'Original global orientation changed');
assert(maxOriginalBonePositionError<1e-6,'Original joint positions changed');
assert(!next.g.accessors.some(a=>a.sparse),'Dense morph accessors required by runtime');
const roll=next.g.animations.find(a=>a.name==='UAL_roll'),morphChannel=roll.channels.find(c=>c.target.path==='weights'),targetTimes=next.acc(roll.samplers[morphChannel.sampler].input).flat();
for(const n of next.g.nodes)if(n.mesh!==undefined){assert(n.weights.every(w=>w===0));const primitives=next.g.meshes[n.mesh].primitives;for(const p of primitives){assert.equal(p.targets.length,targetTimes.length);for(const target of p.targets)for(const attribute of Object.values(target))next.acc(attribute);}}
for(const a of next.g.animations){const channels=a.channels.filter(c=>c.target.path==='weights');assert.equal(channels.length,a.name==='UAL_roll'?12:0);}
// Check half-frame interpolation too: inverse-LBS corrective keys can overshoot
// when their matrices approach singularity, even if all original samples are right.
const transform=(m,p)=>[0,1,2].map(r=>m[r]*p[0]+m[4+r]*p[1]+m[8+r]*p[2]+m[12+r]);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],sub=(a,b)=>a.map((x,i)=>x-b[i]);
const area=p=>(Math.hypot(...cross(sub(p[1],p[0]),sub(p[2],p[0])))+Math.hypot(...cross(sub(p[2],p[0]),sub(p[3],p[0]))))/2;
const cached=[];
for(const node of next.g.nodes)if(node.mesh!==undefined&&!node.name.endsWith('_outer')){
 const p=next.g.meshes[node.mesh].primitives[0],positions=next.acc(p.attributes.POSITION),indices=next.acc(p.attributes.JOINTS_0),weights=next.acc(p.attributes.WEIGHTS_0),targets=p.targets.map(t=>next.acc(t.POSITION)),levels=new Map();
 positions.forEach((p,i)=>{const key=p[1].toFixed(5);if(!levels.has(key))levels.set(key,new Map());levels.get(key).set(p[0].toFixed(5)+'/'+p[2].toFixed(5),i);});
 const rings=[...levels.values()].filter(v=>v.size===4).map(v=>{const ids=[...v.values()],cx=ids.reduce((s,i)=>s+positions[i][0]/4,0),cz=ids.reduce((s,i)=>s+positions[i][2]/4,0);ids.sort((i,j)=>Math.atan2(positions[i][2]-cz,positions[i][0]-cx)-Math.atan2(positions[j][2]-cz,positions[j][0]-cx));return ids;});cached.push({name:node.name,positions,indices,weights,targets,rings});
}
let minInterpolatedArea=Infinity,maxInterpolatedArea=0;const interpolation=[];
for(let sample=0;sample<705;sample++){
 const time=sample/480;let frame=0;while(frame+1<targetTimes.length&&targetTimes[frame+1]<=time+1e-7)frame++;const fraction=frame===targetTimes.length-1?0:Math.max(0,(time-targetTimes[frame])/(targetTimes[frame+1]-targetTimes[frame])),global=next.pose('UAL_roll',time),matrices=skin.joints.map((j,i)=>mul(global[j],ibm[i]));let minimum=Infinity,maximum=0;
 for(const mesh of cached){const evaluated=mesh.positions.map((p,v)=>{const point=p.map((x,k)=>x+mesh.targets[frame][v][k]*(1-fraction)+mesh.targets[Math.min(targetTimes.length-1,frame+1)][v][k]*fraction),out=[0,0,0];for(let k=0;k<4;k++)if(mesh.weights[v][k]>0){const q=transform(matrices[mesh.indices[v][k]],point);for(let a=0;a<3;a++)out[a]+=q[a]*mesh.weights[v][k];}return out;});for(const ring of mesh.rings){const ratio=area(ring.map(i=>evaluated[i]))/area(ring.map(i=>mesh.positions[i]));minimum=Math.min(minimum,ratio);maximum=Math.max(maximum,ratio);}}
 minInterpolatedArea=Math.min(minInterpolatedArea,minimum);maxInterpolatedArea=Math.max(maxInterpolatedArea,maximum);interpolation.push({sourceFrame:1+sample/16,time,minAreaRatio:minimum,maxAreaRatio:maximum});
}
fs.writeFileSync(path.join(root,'animation_pipeline/reports/M002_R3_1_INTERPOLATION.json'),JSON.stringify({minInterpolatedArea,maxInterpolatedArea,samples:interpolation},null,2));
assert(minInterpolatedArea>.9&&maxInterpolatedArea<1.1,'Corrective interpolation needs more corrective samples');
const refinement=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R3_1_TOPOLOGY.json')));assert(refinement.meshes.every(m=>m.boundaryEdges===0));
const result={status:'PASS',preservedFiles:Object.keys(baseline).length,joints:12,weightedVertices:weighted,blendedVertices:blended,torsoBlendedVertices:torsoBlended,maxWeightError,maxBindError,maxRestBoundsError,originalUvCornersPreserved:true,torsoOriginalUvCornersPreserved:true,unitNormals:true,maxOldGlobalOrientationMatrixError:maxOldOrientationError,maxOriginalBonePositionError,durations,morphTargetsPerMesh:targetTimes.length,minInterpolatedArea,maxInterpolatedArea,interpolationSamples:705,morphDense:true,correctiveChannelsOnlyInRoll:true,allSurfacesClosed:true,hipSeam:'shared pelvis weights on torso bottom and leg top rings; added intermediate ring',limit:'closed topology does not rule out self-intersection; original joint transforms preserved'};
fs.writeFileSync(path.join(root,'animation_pipeline/reports/M002_R3_1_VALIDATION.json'),JSON.stringify(result,null,2));console.log(result);
