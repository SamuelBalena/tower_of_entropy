// Derive only Slim arm positions and UVs. Classic and all animation/skeleton bytes stay intact.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const root=path.resolve(__dirname,'../..'),src=path.join(root,'animation_pipeline/converted/m002_a3/resources/assets/towerofentropy/models/gltf/steve_a3.glb');
const b=fs.readFileSync(src),jl=b.readUInt32LE(12),g=JSON.parse(b.subarray(20,20+jl)),original=structuredClone(g),bin=Buffer.from(b.subarray(28+jl));
const at=(id,i,k)=>{const a=g.accessors[id],v=g.bufferViews[a.bufferView],w={VEC2:2,VEC3:3}[a.type];assert.equal(a.componentType,5126);return(v.byteOffset||0)+(a.byteOffset||0)+i*(v.byteStride||w*4)+k*4;};
const read=(id,i,k)=>bin.readFloatLE(at(id,i,k)),write=(id,i,k,x)=>bin.writeFloatLE(x,at(id,i,k));
let vertices=0;
for(const m of g.meshes.filter(m=>/^limb_[23](_outer)?$/.test(m.name))){
 const left=m.name.startsWith('limb_2'),outer=m.name.endsWith('outer'),u=left?(outer?48:32):40,v=left?48:(outer?32:16),inflate=outer?.25:0;
 for(const p of m.primitives){const pos=p.attributes.POSITION,uv=p.attributes.TEXCOORD_0,norm=p.attributes.NORMAL,a=g.accessors[pos];
  const min=left?4-inflate:-8-inflate,max=left?8+inflate:-4+inflate,newMin=left?min:min+1,newMax=left?max-1:max;
  for(let i=0;i<a.count;i++){
   const px=read(pos,i,0)*16;write(pos,i,0,(newMin+(px-min)/(max-min)*(newMax-newMin))/16);
   const x=read(uv,i,0)*64,y=read(uv,i,1)*64,nx=read(norm,i,0),ny=read(norm,i,1),nz=read(norm,i,2);
   let oldStart,newStart,width;
   if(Math.abs(ny)>.9){oldStart=ny<0?u+8:u+4;newStart=ny<0?u+7:u+4;width=3;}
   else if(nz<-.9){oldStart=u+4;newStart=u+4;width=3;}
   else if(nx>.9){oldStart=u+8;newStart=u+7;width=4;}
   else if(nz>.9){oldStart=u+12;newStart=u+11;width=3;}
   else{oldStart=u;newStart=u;width=4;}
   write(uv,i,0,(newStart+(x-oldStart)*width/4)/64);
   assert(Number.isFinite(y));vertices++;
  }
  const xs=Array.from({length:a.count},(_,i)=>read(pos,i,0));a.min[0]=Math.min(...xs);a.max[0]=Math.max(...xs);
 }
}
for(const k of ['nodes','skins','animations','materials','images','textures'])assert.deepEqual(g[k],original[k],k);
let j=Buffer.from(JSON.stringify(g));j=Buffer.concat([j,Buffer.alloc((4-j.length%4)%4,32)]);
const header=Buffer.alloc(20),bh=Buffer.alloc(8);header.writeUInt32LE(0x46546c67);header.writeUInt32LE(2,4);header.writeUInt32LE(28+j.length+bin.length,8);header.writeUInt32LE(j.length,12);header.writeUInt32LE(0x4e4f534a,16);bh.writeUInt32LE(bin.length);bh.writeUInt32LE(0x004e4942,4);
const target=path.join(root,'animation_pipeline/converted/m002_a3_1/resources/assets/towerofentropy/models/gltf');fs.mkdirSync(target,{recursive:true});fs.writeFileSync(path.join(target,'steve_a3_slim.glb'),Buffer.concat([header,j,bh,bin]));
console.log('Slim derived: 3px arms, remapped 64x64 face islands; '+vertices+' vertices; Classic SHA256 '+crypto.createHash('sha256').update(b).digest('hex'));
