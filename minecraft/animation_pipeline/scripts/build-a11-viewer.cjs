const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const dir=path.resolve(__dirname,'../reports/m002_a1_1'),data=JSON.parse(fs.readFileSync(path.join(dir,'RUNTIME.json')));
// Join captured client-world ticks to actual server impact attempts in the same run.
const raw=fs.readFileSync(path.resolve(__dirname,'../../.logs/m002-a11-client.log'));
const log=raw.toString(raw[0]===255&&raw[1]===254?'utf16le':'utf8');
const impacts=[...log.matchAll(/TOE_M002_0 IMPACT player=\S+ start=(\d+) tick=(\d+) target=(-?\d+) damaged=(true|false)/g)].map(m=>({start:+m[1],tick:+m[2],target:+m[3],damaged:m[4]==='true'}));
for(const s of data.series)for(const f of s.frames){const hits=impacts.filter(h=>h.tick>=s.frames[0].tick&&h.tick<=f.tick);f.serverImpacts=hits.length;f.serverImpactTick=hits.at(-1)?.tick??-1;}
fs.writeFileSync(path.join(dir,'VIEWER_TIMELINE.json'),JSON.stringify(data.series));
let pngs=0;for(const s of data.series){assert(s.frames.length>1,s.name);for(const f of s.frames){assert(fs.existsSync(path.join(dir,f.src)),f.src+' missing');pngs++;}}
const template=fs.readFileSync(path.join(__dirname,'a11-viewer.html'),'utf8');
fs.writeFileSync(path.join(dir,'VIEWER.html'),template.replace('__DATA__',JSON.stringify(data.series).replaceAll('<','\\u003c')));
const report={series:data.series.length,pngs,framesPerSecond:20,timing:'Client tick timestamps, 50 ms nominal; discrete PNG playback, not continuous video',angles:['front','side','back'],videoEncoderInstalled:false};
fs.writeFileSync(path.join(dir,'EVIDENCE.json'),JSON.stringify(report,null,2));console.log(report);
