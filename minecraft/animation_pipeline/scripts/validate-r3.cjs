const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const root=path.resolve(__dirname,'../..'),hash=p=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex');
const baseline=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R3_BASELINE.json')));for(const[p,h]of Object.entries(baseline))assert.equal(hash(p),h,p);
function load(rel){const b=fs.readFileSync(path.join(root,rel));let g,bin;for(let o=12;o<b.length;){const n=b.readUInt32LE(o),t=b.readUInt32LE(o+4);o+=8;if(t===0x4e4f534a)g=JSON.parse(b.subarray(o,o+n));else if(t===0x004e4942)bin=b.subarray(o,o+n);o+=n;}
 function acc(id){const a=g.accessors[id],v=g.bufferViews[a.bufferView],w={SCALAR:1,VEC2:2,VEC3:3,VEC4:4,MAT4:16}[a.type],bytes={5121:1,5123:2,5125:4,5126:4}[a.componentType];assert(w&&bytes);return Array.from({length:a.count},(_,i)=>Array.from({length:w},(_,k)=>{const o=(v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||w*bytes)+k*bytes;let x=a.componentType===5126?bin.readFloatLE(o):a.componentType===5121?bin.readUInt8(o):a.componentType===5123?bin.readUInt16LE(o):bin.readUInt32LE(o);if(a.normalized)x/=a.componentType===5121?255:65535;assert(Number.isFinite(x));return x;}));}
 const parents={};g.nodes.forEach((n,i)=>(n.children||[]).forEach(j=>parents[j]=i));const animation=Object.fromEntries(g.animations.map(a=>[a.name,a.channels.map(c=>{const s=a.samplers[c.sampler];return {node:c.target.node,path:c.target.path,t:acc(s.input).flat(),v:acc(s.output),mode:s.interpolation||'LINEAR'};})]));
 function pose(name,t){const l=g.nodes.map(n=>({p:[...(n.translation||[0,0,0])],q:[...(n.rotation||[0,0,0,1])],s:[...(n.scale||[1,1,1])]}));for(const c of animation[name]||[]){let i=0;while(i+1<c.t.length&&c.t[i+1]<=t+1e-7)i++;const f=i===c.t.length-1||c.mode==='STEP'?0:Math.max(0,(t-c.t[i])/(c.t[i+1]-c.t[i]));const value=f===0?c.v[i]:c.path==='rotation'?slerp(c.v[i],c.v[i+1],f):c.v[i].map((v,k)=>v+(c.v[i+1][k]-v)*f);l[c.node][{translation:'p',rotation:'q',scale:'s'}[c.path]]=value;}const world=[];function get(i){if(world[i])return world[i];const m=g.nodes[i].matrix||compose(l[i]);return world[i]=parents[i]===undefined?m:mul(get(parents[i]),m);}g.nodes.forEach((_,i)=>get(i));return world;}
 return {g,acc,pose,index:n=>g.nodes.findIndex(x=>x.name===n)};
}
function slerp(a,b,t){let d=a.reduce((s,v,i)=>s+v*b[i],0);if(d<0){b=b.map(v=>-v);d=-d;}let q;if(d>.9995)q=a.map((v,i)=>v+(b[i]-v)*t);else{const h=Math.acos(Math.min(1,d));q=a.map((v,i)=>(v*Math.sin((1-t)*h)+b[i]*Math.sin(t*h))/Math.sin(h));}const n=Math.hypot(...q);return q.map(v=>v/n);}
function compose({p,q,s}){const[x,y,z,w]=q;return [(1-2*y*y-2*z*z)*s[0],(2*x*y+2*w*z)*s[0],(2*x*z-2*w*y)*s[0],0,(2*x*y-2*w*z)*s[1],(1-2*x*x-2*z*z)*s[1],(2*y*z+2*w*x)*s[1],0,(2*x*z+2*w*y)*s[2],(2*y*z-2*w*x)*s[2],(1-2*x*x-2*y*y)*s[2],0,...p,1];}
function mul(a,b){return Array.from({length:16},(_,i)=>{const row=i%4,col=Math.floor(i/4);let v=0;for(let k=0;k<4;k++)v+=a[k*4+row]*b[col*4+k];return v;});}
const old=load('animation_pipeline/converted/m002_r1/steve-r1-weighted.glb'),next=load('animation_pipeline/converted/m002_r3/steve-r3-refined.glb');assert.equal(next.g.animations.length,4);assert.equal(next.g.skins[0].joints.length,12);
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
 if(!n.name.startsWith('body'))assert.deepEqual(keys,oldKeys,'Unchanged mesh UVs: '+n.name);
 else for(const k of new Set(oldKeys))assert(keys.includes(k),'Original torso UV corner missing');
 for(const normal of next.acc(p.attributes.NORMAL))assert(Math.abs(Math.hypot(...normal)-1)<1e-5);
 for(const indices of next.acc(p.attributes.JOINTS_0))assert(indices.every(i=>i>=0&&i<12));
}
assert(maxRestBoundsError<1e-6,'Classic proportions changed');
let maxBindError=0;const bind=next.pose('',0),skin=next.g.skins[0],ibm=next.acc(skin.inverseBindMatrices);for(let i=0;i<skin.joints.length;i++){const m=mul(bind[skin.joints[i]],ibm[i]);for(let k=0;k<16;k++)maxBindError=Math.max(maxBindError,Math.abs(m[k]-(k%5===0?1:0)));}assert(maxBindError<1e-5);
let maxOldOrientationError=0;const durations={};
const oldNames=['0','1','2','3','4','5','102','103','104','105'];
for(const a of next.g.animations){const times=next.acc(a.samplers[0].input).flat(),end=times.at(-1);const previous=old.g.animations.find(x=>x.name===a.name);const oldEnd=old.acc(previous.samplers[0].input).flat().at(-1);assert(Math.abs(end-oldEnd)<1e-6);durations[a.name]=end;
 for(const t of times){const om=old.pose(a.name,t),nm=next.pose(a.name,t);for(const name of oldNames){const m=om[old.index(name)],n=nm[next.index(name)];for(let col=0;col<3;col++){const lm=Math.hypot(m[col*4],m[col*4+1],m[col*4+2]),ln=Math.hypot(n[col*4],n[col*4+1],n[col*4+2]);for(let row=0;row<3;row++)maxOldOrientationError=Math.max(maxOldOrientationError,Math.abs(m[col*4+row]/lm-n[col*4+row]/ln));}}}
}
assert(maxOldOrientationError<2e-4,'Original global orientation changed');
const refinement=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R3_REFINEMENT.json')));assert(refinement.meshes.every(m=>m.boundaryEdges===0));
const result={status:'PASS',preservedFiles:Object.keys(baseline).length,joints:12,weightedVertices:weighted,blendedVertices:blended,torsoBlendedVertices:torsoBlended,maxWeightError,maxBindError,maxRestBoundsError,unchangedLimbHeadUvs:true,torsoOriginalUvCornersPreserved:true,unitNormals:true,maxOldGlobalOrientationMatrixError:maxOldOrientationError,durations,allSurfacesClosed:true,hipSeam:'shared pelvis weights on torso bottom and leg top rings',limit:'closed topology does not rule out self-intersection or volume loss at every frame'};
fs.writeFileSync(path.join(root,'animation_pipeline/reports/M002_R3_VALIDATION.json'),JSON.stringify(result,null,2));console.log(result);
