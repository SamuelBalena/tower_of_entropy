const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const base=path.resolve(__dirname,'../converted');
const parkour=process.argv.includes('--parkour');
const prototypes=process.argv.includes('--wall-prototypes');
const expansion=path.join(base,prototypes?'wall-prototypes':parkour?'parkour':'expansion');
const target=path.join(expansion,'resources/assets/towerofentropy/models/gltf');

function read(file){
    const data=fs.readFileSync(file);
    const jsonSize=data.readUInt32LE(12);
    assert.equal(data.readUInt32LE(0),0x46546c67);
    return {json:JSON.parse(data.subarray(20,20+jsonSize)),bin:data.subarray(28+jsonSize)};
}
function write(file,json,bin){
    const raw=Buffer.from(JSON.stringify(json));
    const padded=Buffer.concat([raw,Buffer.alloc((4-raw.length%4)%4,32)]);
    const header=Buffer.alloc(20),chunk=Buffer.alloc(8);
    header.writeUInt32LE(0x46546c67);
    header.writeUInt32LE(2,4);
    header.writeUInt32LE(28+padded.length+bin.length,8);
    header.writeUInt32LE(padded.length,12);
    header.writeUInt32LE(0x4e4f534a,16);
    chunk.writeUInt32LE(bin.length);
    chunk.writeUInt32LE(0x004e4942,4);
    fs.writeFileSync(file,Buffer.concat([header,padded,chunk,bin]));
}
const motion=read(path.join(expansion,'motion-only.glb'));
const additions=motion.json.animations;
assert.deepEqual(additions.map(a=>a.name).sort(), prototypes ? ['UAL_ledge_climb_ual2','UAL_wall_slide_push'] : parkour
    ? ['UAL_ledge_climb','UAL_ledge_hang','UAL_punch_cross_front','UAL_punch_jab_front','UAL_wall_jump','UAL_wall_slide']
    : ['UAL_punch_cross','UAL_punch_jab']);
fs.mkdirSync(target,{recursive:true});

for(const [folder,name] of [['m002_a3','steve_a3'],['m002_a3_1','steve_a3_slim']]){
    const original=read(path.join(base,prototypes?'parkour':parkour?'expansion':folder,'resources/assets/towerofentropy/models/gltf',name+(prototypes?'_parkour':parkour?'_expansion':'')+'.glb'));
    const json=structuredClone(original.json);
    const chunks=[original.bin];
    let offset=original.bin.length;
    const mapped=new Map();
    function appendAccessor(id){
        if(mapped.has(id))return mapped.get(id);
        const acc=motion.json.accessors[id];
        const view=motion.json.bufferViews[acc.bufferView];
        assert.equal(view.buffer,0);
        assert.equal(view.byteLength%4,0);
        const start=view.byteOffset||0;
        chunks.push(motion.bin.subarray(start,start+view.byteLength));
        const bufferView=json.bufferViews.push({...view,byteOffset:offset})-1;
        offset+=view.byteLength;
        const index=json.accessors.push({...acc,bufferView})-1;
        mapped.set(id,index);
        return index;
    }
    for(const clip of additions){
        const copy=structuredClone(clip);
        for(const sampler of copy.samplers){
            sampler.input=appendAccessor(sampler.input);
            sampler.output=appendAccessor(sampler.output);
        }
        for(const channel of copy.channels){
            const node=motion.json.nodes[channel.target.node].name;
            channel.target.node=json.nodes.findIndex(n=>n.name===node);
            assert(channel.target.node>=0,node);
        }
        json.animations.push(copy);
    }
    const bin=Buffer.concat(chunks);
    json.buffers[0].byteLength=bin.length;
    const output=path.join(target,name+(prototypes?'_wall_prototypes':parkour?'_parkour':'_expansion')+'.glb');
    write(output,json,bin);
    const packed=read(output);
    for(const key of ['nodes','meshes','skins','materials','textures','images']){
        assert.deepEqual(packed.json[key],original.json[key],key);
    }
    for(const key of ['accessors','bufferViews','animations']){
        assert.deepEqual(packed.json[key].slice(0,original.json[key].length),original.json[key],key);
    }
    assert(packed.bin.subarray(0,original.bin.length).equals(original.bin));
    console.log(output+' verified');
}
const poses=JSON.parse(fs.readFileSync(path.join(expansion,'poses-new.json'),'utf8'));
for(const clip of Object.values(poses)){
    assert(clip.frames.every(f=>f.components.some(c=>c.storeID===303)));
}
fs.writeFileSync(path.join(target,prototypes?'../../wall-prototype-poses.json':parkour?'../../parkour-poses.json':'../../expansion-poses.json'),JSON.stringify(poses));
