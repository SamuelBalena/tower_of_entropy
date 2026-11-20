const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const base = path.resolve(__dirname, '../converted');
const out = path.join(base, 'parkour');
const original = JSON.parse(fs.readFileSync(path.join(base, 'expansion/poses-new.json')));
const identity = [0,0,0,1];
const neutralSpine = {quaternions:{pelvis:identity,spine_02:identity}};
const poses = {};

function keyframe(angles) {
    const frame = structuredClone(original.punch_jab.frames[0]);
    for (const part of frame.components) {
        const [x,y,z] = angles[part.storeID] || [0,0,0];
        part.rotation = {x,y,z};
        if (part.storeID === 302 || part.storeID === 303) {
            part.pos = {x:0,y:-4,z:0};
        }
    }
    return frame;
}

function clip(name, keys, loop = false) {
    const last = keys.at(-1)[0];
    const frames = [];
    let segment = 0;
    for (let i = 0; i <= last; i++) {
        while (i > keys[segment+1][0]) segment++;
        const [start, a] = keys[segment];
        const [end, b] = keys[segment+1];
        const phase = (i-start)/(end-start);
        const t = phase*phase*(3-2*phase);
        const angles = {};
        for (const id of new Set([...Object.keys(a),...Object.keys(b)])) {
            const left = a[id] || [0,0,0], right = b[id] || [0,0,0];
            angles[id] = left.map((v,j)=>v + (right[j]-v)*t);
        }
        frames.push(keyframe(angles));
    }
    poses[name] = {duration:Math.round(last*1000/30), frames, loop,
        spine:frames.map(()=>neutralSpine)};
}

for (const [name,arm,other,forearm,otherForearm,side] of [
    ['punch_jab_front',2,3,102,103,1], ['punch_cross_front',3,2,103,102,-1]
]) {
    const guard = {[arm]:[-38,0,side*7],[forearm]:[-96,0,0],
        [other]:[-34,0,-side*8],[otherForearm]:[-95,0,0]};
    const strike = {...guard,1:[5,side*12,0],
        [arm]:[-82,0,side*5],[forearm]:[-8,0,0]};
    // CPM uses -Z as forward. Require the striking chain to point mostly along that axis.
    const [shoulder,,spread] = strike[arm], [elbow] = strike[forearm];
    const radians = n => n*Math.PI/180;
    const forward = -4*Math.sin(radians(shoulder))-6*Math.sin(radians(shoulder+elbow));
    const lateral = Math.abs(Math.sin(radians(spread)))*10;
    assert(forward > 9 && lateral < 1, name);
    clip(name, [[0,guard],[3,guard],[10,strike],[13,strike],[25,guard]]);
}

const slide = {1:[8,0,0],2:[-110,0,5],3:[-110,0,-5],102:[-18,0,0],
    103:[-18,0,0],4:[-28,0,0],5:[-36,0,0],104:[49,0,0],105:[55,0,0]};
const hang = {1:[2,0,0],2:[-148,0,5],3:[-148,0,-5],102:[-14,0,0],
    103:[-14,0,0],4:[-8,0,0],5:[-13,0,0],104:[15,0,0],105:[22,0,0]};
const pull = {1:[18,0,0],2:[-100,0,5],3:[-100,0,-5],102:[-55,0,0],
    103:[-55,0,0],4:[-64,0,0],5:[-25,0,0],104:[83,0,0],105:[42,0,0]};
const mantle = {1:[32,0,0],2:[-35,0,5],3:[-35,0,-5],102:[-25,0,0],
    103:[-25,0,0],4:[-42,0,0],5:[-57,0,0],104:[64,0,0],105:[82,0,0]};
clip('wall_slide',[[0,slide],[12,{...slide,4:[-34,0,0],5:[-30,0,0]}],[24,slide]],true);
clip('ledge_hang',[[0,hang],[18,hang]],true);
clip('ledge_climb',[[0,hang],[9,pull],[19,mantle],[27,{}]]);
clip('wall_jump',[[0,slide],[4,pull],[10,{2:[-142,0,5],3:[-112,0,-5],
    4:[-28,0,0],5:[16,0,0],104:[28,0,0],105:[22,0,0]}],[19,{}]]);

fs.mkdirSync(out, {recursive:true});
fs.writeFileSync(path.join(out, 'poses-new.json'), JSON.stringify(poses));
