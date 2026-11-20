// Append animation buffers only. Original mesh/skin/node/UV bytes remain intact.
const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'../..'),p=path.join(root,'animation_pipeline');
function read(file){const b=fs.readFileSync(file);let g,bin;for(let o=12;o<b.length;){const n=b.readUInt32LE(o),t=b.readUInt32LE(o+4);o+=8;if(t===0x4e4f534a)g=JSON.parse(b.subarray(o,o+n));else if(t===0x004e4942)bin=b.subarray(o,o+n);o+=n;}return {g,bin};}
const original=read(path.join(p,'converted/m002_r3/steve-r3-refined.glb')),extra=read(path.join(p,'converted/m002_a1/motion-only.glb'));
const g=structuredClone(original.g),offset=original.bin.length,views=g.bufferViews.length,accs=g.accessors.length;
for(const v of extra.g.bufferViews)g.bufferViews.push({...v,byteOffset:(v.byteOffset||0)+offset});
for(const a of extra.g.accessors)g.accessors.push({...a,bufferView:a.bufferView+views});
for(const a of extra.g.animations.filter(a=>['UAL_sword_b','UAL_sword_c','UAL_heavy'].includes(a.name))){
 const copy=structuredClone(a);
 for(const s of copy.samplers){s.input+=accs;s.output+=accs;}
 for(const c of copy.channels){const name=extra.g.nodes[c.target.node].name;const id=g.nodes.findIndex(n=>n.name===name);assert(id>=0,name);c.target.node=id;}
 g.animations.push(copy);
}
const bin=Buffer.concat([original.bin,extra.bin]);g.buffers=[{byteLength:bin.length}];
for(const key of ['nodes','meshes','skins','materials','textures','images'])assert.deepEqual(g[key],original.g[key],key);
assert.deepEqual(g.animations.slice(0,original.g.animations.length),original.g.animations);
let json=Buffer.from(JSON.stringify(g));json=Buffer.concat([json,Buffer.alloc((4-json.length%4)%4,32)]);
const header=Buffer.alloc(12),jh=Buffer.alloc(8),bh=Buffer.alloc(8);header.writeUInt32LE(0x46546c67);header.writeUInt32LE(2,4);header.writeUInt32LE(28+json.length+bin.length,8);jh.writeUInt32LE(json.length);jh.writeUInt32LE(0x4e4f534a,4);bh.writeUInt32LE(bin.length);bh.writeUInt32LE(0x004e4942,4);
const target=path.join(p,'converted/m002_r4/resources/assets/towerofentropy');
fs.writeFileSync(path.join(target,'models/gltf/steve_r3.glb'),Buffer.concat([header,jh,json,bh,bin]));
const poses=JSON.parse(fs.readFileSync(path.join(p,'converted/m002_r1/poses.json'))),added=JSON.parse(fs.readFileSync(path.join(p,'converted/m002_a1/poses-new.json')));
for(const key of ['sword_b','sword_c','heavy'])poses[key]=added[key];
fs.writeFileSync(path.join(target,'r3-poses.json'),JSON.stringify(poses));
console.log('A1 appended B/C/heavy; approved mesh, rig, UVs and original four animations unchanged.');
