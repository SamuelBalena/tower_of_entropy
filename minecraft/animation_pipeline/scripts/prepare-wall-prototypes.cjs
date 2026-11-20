const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const pipeline = path.resolve(__dirname, '..');
const assets = path.resolve(pipeline, '../../assets/source');
const out = path.join(pipeline, 'converted/wall-prototypes');
const parkour = JSON.parse(fs.readFileSync(path.join(pipeline, 'converted/parkour/poses-new.json')));
const existingSampler = fs.readFileSync(path.join(__dirname, 'retarget-idle.cjs'), 'utf8');
const sampler = existingSampler.slice(existingSampler.indexOf('let g,bin;'), existingSampler.indexOf('const face='))
    .replace(/const clip=roll\?.*?;/, 'const clip = sourceClip;')
    .replace(/if\(Math\.abs\(duration-.*?throw Error\('Unexpected duration'\);/, '');

const specs = [
    ['wall_slide_push', 'Universal Animation Library[Standard]', 'UAL1_Standard.glb', 'Push_Loop', 80/30, true],
    ['ledge_climb_ual2', 'Universal Animation Library 2[Standard]', 'UAL2_Standard.glb', 'ClimbUp_1m', 20/30, false]
];
const signed = n => ((n + 540) % 360) - 180;
const mix = (a,b,t) => a + (b-a)*t;
const smooth = n => {
    const t = Math.max(0, Math.min(1, n));
    return t*t*(3-2*t);
};
const radians = n => n * Math.PI / 180;
const part = (frame,id) => frame.components.find(c=>c.storeID===id);
function baseFrame(key,phase) {
    const clip = parkour[key];
    const index = phase * (clip.frames.length-1);
    const a = clip.frames[Math.floor(index)], b = clip.frames[Math.ceil(index)];
    const frame = structuredClone(a);
    for (const c of frame.components) {
        for (const axis of ['x','y','z']) {
            const other = part(b,c.storeID);
            c.rotation[axis] += (index%1)*signed(other.rotation[axis]-c.rotation[axis]);
        }
    }
    return frame;
}
function blend(frame,source,id,weight) {
    const rotation = part(frame,id).rotation;
    const target = part(source,id).rotation;
    for (const axis of ['x','y','z']) rotation[axis] += signed(target[axis] - rotation[axis])*weight;
}
// The nominal wall plane is about 5.1 rig units from the player center.
// Limit forward reach using a sagittal arm envelope plus padding for the block wrist.
function constrainPush(frame, id, forearm) {
    const upper = part(frame,id).rotation, lower = part(frame,forearm).rotation;
    lower.x = Math.max(-112, Math.min(-42, signed(lower.x)));
    upper.z = Math.max(-10, Math.min(10, signed(upper.z)));
    upper.y = Math.max(-12, Math.min(12, signed(upper.y)));
    const reach = pitch => -4*Math.sin(radians(pitch)) - 6*Math.sin(radians(pitch+lower.x));
    let angle = signed(upper.x);
    // Retract a long arm by lifting the shoulder, retaining the source elbow rhythm.
    while (reach(angle) > 1.95 && angle > -170) angle -= 1;
    upper.x = angle;
    assert(reach(angle) <= 1.96 && reach(angle) > .2, 'Push wrist exceeds the intended wall reach');
}

const clips = {}, manifest = [];
for (const [key,dir,file,sourceClip,expected,loop] of specs) {
    const source = path.join(assets,dir,dir,'Unreal-Godot',file);
    const b = fs.readFileSync(source);
    const {duration,rotations,channels,g} = vm.runInNewContext(
        sampler + '\n({duration,rotations,channels,g});',
        {b,sourceClip,process:{argv:[]},roll:false,sword:true,action:true});
    assert(Math.abs(duration-expected)<1e-5, sourceClip+' duration');
    assert.equal(g.skins[0].joints.length,65);
    const rootMotion = channels.find(c=>g.nodes[c.node].name==='root' && c.path==='translation');
    assert(rootMotion && rootMotion.values.every(v=>v.every((n,i)=>Math.abs(n-rootMotion.values[0][i])<1e-6)));
    const length = Math.round((loop ? duration : .9)*30);
    const frames = [], spine = [];
    for (let i=0;i<=length;i++) {
        const phase = i/length;
        // Keep the authored vertical/over-edge split (11 and 7 ticks), then stand.
        const sourcePhase = loop ? phase : phase <= 11/18
            ? mix(0,.57,phase/(11/18))
            : mix(.57,1,(phase-11/18)/(7/18));
        const sampled = {components:rotations(sourcePhase*duration)};
        const frame = baseFrame(loop ? 'wall_slide' : 'ledge_climb',phase);
        if (loop) {
            blend(frame,sampled,1,.045);
            for (const [arm,elbow] of [[2,102],[3,103]]) {
                blend(frame,sampled,arm,.35);
                blend(frame,sampled,elbow,.45);
                constrainPush(frame,arm,elbow);
            }
        } else {
            // Keep the approved hang and standing ends, and its clearance-oriented torso.
            const weight = smooth(phase/.20)*smooth((1-phase)/.22);
            for (const id of [2,3,102,103]) blend(frame,sampled,id,.48*weight);
            for (const id of [0,1,4,5,104,105]) blend(frame,sampled,id,.27*weight);
        }
        frames.push(frame);
        spine.push({quaternions:{pelvis:[0,0,0,1],spine_02:[0,0,0,1]}});
    }
    if(loop) frames[length]=structuredClone(frames[0]);
    clips[key]={duration:Math.round(length*1000/30),frames,spine,loop};
    manifest.push({clip:key,source:sourceClip,file:path.relative(pipeline,source),duration});
}
fs.mkdirSync(out,{recursive:true});
fs.writeFileSync(path.join(out,'poses-new.json'),JSON.stringify(clips));
fs.writeFileSync(path.join(out,'selected-assets.json'),JSON.stringify(manifest,null,2));
console.log(manifest);
