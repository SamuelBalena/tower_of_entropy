const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'animation_pipeline/converted/m002_r1');
const base=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/reports/M002_R1_BASELINE.json')));
for(const[p,h]of Object.entries(base))assert.equal(crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex'),h,p);
const original=JSON.parse(fs.readFileSync(path.join(root,'animation_pipeline/converted/m001_3/actions-data.json'))),fixed=JSON.parse(fs.readFileSync(path.join(out,'fixed-data.json')));
for(const[k,v]of Object.entries(original))if(k.startsWith('animations/'))assert.deepEqual(fixed[k],v,'Animation altered');
assert.deepEqual(fixed['config.json'].skinSize,{x:64,y:64});
const buf=fs.readFileSync(path.join(out,'steve-r1-weighted.glb'));let j,bin;
for(let o=12;o<buf.length;){const n=buf.readUInt32LE(o),t=buf.readUInt32LE(o+4);o+=8;if(t===0x4e4f534a)j=JSON.parse(buf.subarray(o,o+n));else if(t===0x004e4942)bin=buf.subarray(o,o+n);o+=n;}
function acc(id){const a=j.accessors[id],v=j.bufferViews[a.bufferView],width={SCALAR:1,VEC2:2,VEC3:3,VEC4:4}[a.type],bytes={5121:1,5123:2,5125:4,5126:4}[a.componentType];assert(width&&bytes);const r=[];
for(let i=0;i<a.count;i++){const row=[];for(let k=0;k<width;k++){const o=(v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||width*bytes)+k*bytes;let val=a.componentType===5126?bin.readFloatLE(o):a.componentType===5121?bin.readUInt8(o):a.componentType===5123?bin.readUInt16LE(o):bin.readUInt32LE(o);if(a.normalized)val/=a.componentType===5121?255:65535;assert(Number.isFinite(val));row.push(val);}r.push(row);}return r;}
let weightedVertices=0,blendedVertices=0;
for(const mesh of j.meshes)for(const p of mesh.primitives){assert(p.attributes.WEIGHTS_0!==undefined);for(const w of acc(p.attributes.WEIGHTS_0)){assert(Math.abs(w.reduce((a,b)=>a+b,0)-1)<1e-5);weightedVertices++;if(w.filter(x=>x>0).length>1)blendedVertices++;}for(const uv of acc(p.attributes.TEXCOORD_0))assert(uv.every(x=>x>=0&&x<=1));}
assert(blendedVertices>0);assert.equal(j.skins.length,1);assert.equal(j.animations.length,4);
const expected={UAL_idle:2.5,UAL_jog:28/30,UAL_sword:13/30,UAL_roll:44/30},durations={};
for(const a of j.animations){let end=0;for(const s of a.samplers)end=Math.max(end,...acc(s.input).flat());assert(Math.abs(end-expected[a.name])<1e-5,a.name+' timing');durations[a.name]=end;}
const result={status:'PASS',approvedFilesUnchanged:Object.keys(base).length,animationData:'all four original CPM arrays unchanged',skin:[64,64],weightedVertices,blendedVertices,joints:j.skins[0].joints.length,durations,limit:'Offline GLB only; not accepted as a weighted model by current CPM or Full Motion APIs'};
fs.writeFileSync(path.join(root,'animation_pipeline/reports/M002_R1_VALIDATION.json'),JSON.stringify(result,null,2));console.log(result);
